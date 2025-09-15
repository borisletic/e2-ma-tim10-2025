package com.example.ma2025.ui.tasks;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ma2025.MainActivity;
import com.example.ma2025.R;
import com.example.ma2025.data.database.entities.TaskEntity;
import com.example.ma2025.viewmodels.TaskListViewModel;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

public class TaskListFragment extends Fragment implements TaskAdapter.OnTaskActionListener {

    private RecyclerView recyclerView;
    private TaskAdapter taskAdapter;
    private TaskListViewModel viewModel;
    private TabLayout tabLayout;
    private FloatingActionButton fabAddTask;
    private ChipGroup chipGroupTaskType;

    private List<TaskEntity> allTasks = new ArrayList<>();
    private int currentFilter = TaskEntity.STATUS_ACTIVE;
    private int currentTypeFilter = 0; // Tip zadatka: 0 = jednokratni, 1 = ponavljajući

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_task_list, container, false);

        initViews(view);
        setupViewModel();
        setupRecyclerView();
        setupChipGroup();
        setupFab();
        observeData();

        return view;
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.rv_tasks);
        tabLayout = view.findViewById(R.id.tab_layout);
        fabAddTask = view.findViewById(R.id.fab_add_task);
        chipGroupTaskType = view.findViewById(R.id.chip_group_task_type);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(TaskListViewModel.class);
    }

    private void setupRecyclerView() {
        taskAdapter = new TaskAdapter(requireContext(), this);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(taskAdapter);
    }

    private void setupChipGroup() {
        chipGroupTaskType.check(R.id.chip_one_time_tasks);
        currentTypeFilter = 0;
        setupTabsForTaskType();

        chipGroupTaskType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chip_one_time_tasks) {
                currentTypeFilter = 0; // Jednokratni
            } else if (checkedId == R.id.chip_recurring_tasks) {
                currentTypeFilter = 1; // Ponavljajući
            }
            setupTabsForTaskType();
            filterTasks();
        });
    }

    private void setupTabsForTaskType() {
        tabLayout.removeAllTabs();

        if (currentTypeFilter == 0) { // Jednokratni
            tabLayout.addTab(tabLayout.newTab().setText("Aktivni"));
            tabLayout.addTab(tabLayout.newTab().setText("Urađeni"));
            tabLayout.addTab(tabLayout.newTab().setText("Neurađeni"));
            tabLayout.addTab(tabLayout.newTab().setText("Otkazani"));
        } else { // Ponavljajući
            tabLayout.addTab(tabLayout.newTab().setText("Aktivni"));
            tabLayout.addTab(tabLayout.newTab().setText("Urađeni"));
            tabLayout.addTab(tabLayout.newTab().setText("Neurađeni"));
            tabLayout.addTab(tabLayout.newTab().setText("Pauzirani"));
            tabLayout.addTab(tabLayout.newTab().setText("Otkazani"));
        }

        tabLayout.clearOnTabSelectedListeners();
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                String tabText = tab.getText() != null ? tab.getText().toString() : "";
                switch (tabText) {
                    case "Aktivni":
                        currentFilter = TaskEntity.STATUS_ACTIVE;
                        break;
                    case "Urađeni":
                        currentFilter = TaskEntity.STATUS_COMPLETED;
                        break;
                    case "Neurađeni":
                        currentFilter = TaskEntity.STATUS_FAILED;
                        break;
                    case "Pauzirani":
                        currentFilter = TaskEntity.STATUS_PAUSED;
                        break;
                    case "Otkazani":
                        currentFilter = TaskEntity.STATUS_CANCELED;
                        break;
                }
                filterTasks();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        if (tabLayout.getTabCount() > 0) {
            tabLayout.selectTab(tabLayout.getTabAt(0));
        }
    }


    private void setupFab() {
        fabAddTask.setOnClickListener(v -> {
            CreateTaskFragment createFragment = new CreateTaskFragment();
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, createFragment)
                    .addToBackStack(null)
                    .commit();
        });
    }

    private void observeData() {
        String userId = viewModel.getCurrentUserId();
        if (userId != null) {
            viewModel.getAllTasks(userId).observe(getViewLifecycleOwner(), tasks -> {
                if (tasks != null) {
                    // Filtriraj da prikaže samo trenutne i buduće zadatke
                    List<TaskEntity> currentAndFutureTasks = filterCurrentAndFutureTasks(tasks);
                    this.allTasks = currentAndFutureTasks;
                    Log.d("TaskListFragment", "Loaded " + currentAndFutureTasks.size() + " current/future tasks from " + tasks.size() + " total");
                    filterTasks();
                } else {
                    Log.d("TaskListFragment", "No tasks loaded");
                }
            });
        }

        viewModel.getTaskCompletionResult().observe(getViewLifecycleOwner(), result -> {
            if (result != null) {
                if (result.isSuccess()) {
                    Toast.makeText(requireContext(), result.getMessage(), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(), "Greška: " + result.getErrorMessage(), Toast.LENGTH_SHORT).show();
                }
                viewModel.clearResult();
            }
        });
    }

    private void filterTasks() {
        List<TaskEntity> filteredTasks = new ArrayList<>();
        long now = System.currentTimeMillis();

        for (TaskEntity task : allTasks) {
            int effectiveStatus = task.status;
            if (task.status == TaskEntity.STATUS_ACTIVE && task.dueTime != null && task.dueTime < now) {
                effectiveStatus = TaskEntity.STATUS_FAILED;
            }

            boolean matchesStatus = (effectiveStatus == currentFilter); // Uklonite -1 logiku
            boolean matchesType = (currentTypeFilter == 0 && !task.isRepeating)
                    || (currentTypeFilter == 1 && task.isRepeating);

            if (matchesStatus && matchesType) {
                filteredTasks.add(task);
            }
        }

        taskAdapter.updateTasks(filteredTasks);
    }

    private List<TaskEntity> filterCurrentAndFutureTasks(List<TaskEntity> allTasks) {
        List<TaskEntity> currentAndFuture = new ArrayList<>();
        long now = System.currentTimeMillis();
        long startOfToday = now - (now % (24 * 60 * 60 * 1000));

        for (TaskEntity task : allTasks) {
            boolean includeTask = false;

            if (task.dueTime == null) {
                includeTask = true;
            }
            else if (task.dueTime >= startOfToday) {
                includeTask = true;
            }
            else if (task.status == TaskEntity.STATUS_COMPLETED && task.updatedAt >= startOfToday) {
                includeTask = true;
            }

            if (includeTask) {
                currentAndFuture.add(task);
            }
        }

        return currentAndFuture;
    }

    @Override
    public void onTaskClick(TaskEntity task) {
        ((MainActivity) requireActivity()).navigateToTaskDetail(task.id);
    }

    @Override
    public void onTaskComplete(TaskEntity task) {
        long now = System.currentTimeMillis();
        int effectiveStatus = task.status;
        if (task.status == TaskEntity.STATUS_ACTIVE && task.dueTime != null && task.dueTime < now) {
            effectiveStatus = TaskEntity.STATUS_FAILED;
        }
        if (effectiveStatus == TaskEntity.STATUS_ACTIVE) {
            viewModel.completeTask(task.id);
        } else {
            Toast.makeText(requireContext(), "Zadatak nije urađen jer je prošao rok", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onTaskFail(TaskEntity task) {
        long now = System.currentTimeMillis();
        int effectiveStatus = task.status;
        if (task.status == TaskEntity.STATUS_ACTIVE && task.dueTime != null && task.dueTime < now) {
            effectiveStatus = TaskEntity.STATUS_FAILED;
        }
        if (effectiveStatus == TaskEntity.STATUS_ACTIVE) {
            viewModel.failTask(task.id);
        } else {
            Toast.makeText(requireContext(), "Zadatak nije urađen jer je prošao rok", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onTaskCancel(TaskEntity task) {
        if (task != null) viewModel.cancelTask(task.id);
    }

    @Override
    public void onTaskPause(TaskEntity task) {
        if (task != null) {
            if (task.isRepeating) {
                viewModel.pauseTask(task.id);
            } else {
                Toast.makeText(requireContext(), "Samo ponavljajući zadaci mogu biti pauzirani", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onTaskResume(TaskEntity task) {
        if (task != null) viewModel.resumeTask(task.id);
    }

    @Override
    public void onTaskDelete(TaskEntity task) {
        if (task.isRepeating) {
            showDeleteRecurringTaskDialog(task);
        } else {
            viewModel.deleteTask(task);
            Toast.makeText(requireContext(), "Zadatak obrisan", Toast.LENGTH_SHORT).show();
        }
    }

    private void showDeleteRecurringTaskDialog(TaskEntity task) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Brisanje ponavljajućeg zadatka")
                .setMessage("Brisanje ovog zadatka će obrisati i sva buduća ponavljanja. " +
                        "Prethodno završeni zadaci će ostati u kalendaru. Da li želite da nastavite?")
                .setPositiveButton("Obriši sve", (dialog, which) -> {
                    viewModel.deleteRecurringTask(task.id);
                    Toast.makeText(requireContext(), "Ponavljajući zadatak i buduća ponavljanja obrisani", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Otkaži", null)
                .show();
    }
}
