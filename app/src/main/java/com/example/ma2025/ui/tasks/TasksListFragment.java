package com.example.ma2025.ui.tasks;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ma2025.R;
import com.example.ma2025.data.database.entities.TaskEntity;
import com.example.ma2025.data.repositories.TaskRepository;
import com.example.ma2025.utils.Constants;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class TasksListFragment extends Fragment {

    private static final String TAG = "TasksListFragment";

    // Filter types
    private static final int FILTER_ALL = 0;
    private static final int FILTER_SINGLE = 1;
    private static final int FILTER_REPEATING = 2;

    // Views
    private RecyclerView rvTasks;
    private View layoutEmptyState;
    private Button btnAllTasks, btnSingleTasks, btnRepeatingTasks;
    private TextView tvActiveTasks, tvCompletedTasks, tvOverdueTasks;

    // Data
    private TaskRepository taskRepository;
    private TasksListAdapter adapter;
    private String currentUserId;
    private List<TaskEntity> allTasks = new ArrayList<>();
    private int currentFilter = FILTER_ALL;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tasks_list, container, false);

        initializeData();
        initViews(view);
        setupRecyclerView();
        setupListeners();
        loadTasks();

        return view;
    }

    private void initializeData() {
        taskRepository = TaskRepository.getInstance(getContext());
        currentUserId = getCurrentUserId();
    }

    private void initViews(View view) {
        rvTasks = view.findViewById(R.id.rvTasks);
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState);

        btnAllTasks = view.findViewById(R.id.btnAllTasks);
        btnSingleTasks = view.findViewById(R.id.btnSingleTasks);
        btnRepeatingTasks = view.findViewById(R.id.btnRepeatingTasks);

        tvActiveTasks = view.findViewById(R.id.tvActiveTasks);
        tvCompletedTasks = view.findViewById(R.id.tvCompletedTasks);
        tvOverdueTasks = view.findViewById(R.id.tvOverdueTasks);
    }

    private void setupRecyclerView() {
        adapter = new TasksListAdapter(getContext(), new ArrayList<>());
        adapter.setOnTaskActionListener(new TasksListAdapter.OnTaskActionListener() {
            @Override
            public void onTaskClick(TaskEntity task) {
                openTaskDetails(task);
            }

            @Override
            public void onCompleteTask(TaskEntity task) {
                completeTask(task);
            }

            @Override
            public void onTaskAction(TaskEntity task) {
                showTaskActionsDialog(task);
            }
        });

        rvTasks.setLayoutManager(new LinearLayoutManager(getContext()));
        rvTasks.setAdapter(adapter);
    }

    private void setupListeners() {
        btnAllTasks.setOnClickListener(v -> setFilter(FILTER_ALL));
        btnSingleTasks.setOnClickListener(v -> setFilter(FILTER_SINGLE));
        btnRepeatingTasks.setOnClickListener(v -> setFilter(FILTER_REPEATING));
    }

    private void setFilter(int filter) {
        currentFilter = filter;
        updateFilterButtons();
        filterTasks();
    }

    private void updateFilterButtons() {
        // Reset all buttons
        btnAllTasks.setBackgroundResource(R.drawable.card_background);
        btnAllTasks.setTextColor(getResources().getColor(R.color.text_primary));
        btnSingleTasks.setBackgroundResource(R.drawable.card_background);
        btnSingleTasks.setTextColor(getResources().getColor(R.color.text_primary));
        btnRepeatingTasks.setBackgroundResource(R.drawable.card_background);
        btnRepeatingTasks.setTextColor(getResources().getColor(R.color.text_primary));

        // Highlight selected button
        switch (currentFilter) {
            case FILTER_ALL:
                btnAllTasks.setBackgroundColor(getResources().getColor(R.color.primary_color));
                btnAllTasks.setTextColor(getResources().getColor(android.R.color.white));
                break;
            case FILTER_SINGLE:
                btnSingleTasks.setBackgroundColor(getResources().getColor(R.color.primary_color));
                btnSingleTasks.setTextColor(getResources().getColor(android.R.color.white));
                break;
            case FILTER_REPEATING:
                btnRepeatingTasks.setBackgroundColor(getResources().getColor(R.color.primary_color));
                btnRepeatingTasks.setTextColor(getResources().getColor(android.R.color.white));
                break;
        }
    }

    private void filterTasks() {
        List<TaskEntity> filteredTasks = new ArrayList<>();

        for (TaskEntity task : allTasks) {
            // Only show current and future tasks in list (not past ones like in calendar)
            if (task.dueTime != null && task.dueTime < System.currentTimeMillis() && task.status == TaskEntity.STATUS_ACTIVE) {
                continue; // Skip past active tasks
            }

            switch (currentFilter) {
                case FILTER_ALL:
                    filteredTasks.add(task);
                    break;
                case FILTER_SINGLE:
                    if (!task.isRepeating) {
                        filteredTasks.add(task);
                    }
                    break;
                case FILTER_REPEATING:
                    if (task.isRepeating) {
                        filteredTasks.add(task);
                    }
                    break;
            }
        }

        adapter.updateTasks(filteredTasks);
        updateEmptyState(filteredTasks.isEmpty());
        updateStatistics();
    }

    private void updateEmptyState(boolean isEmpty) {
        if (isEmpty) {
            rvTasks.setVisibility(View.GONE);
            layoutEmptyState.setVisibility(View.VISIBLE);
        } else {
            rvTasks.setVisibility(View.VISIBLE);
            layoutEmptyState.setVisibility(View.GONE);
        }
    }

    private void updateStatistics() {
        int activeCount = 0;
        int completedCount = 0;
        int overdueCount = 0;
        long currentTime = System.currentTimeMillis();

        for (TaskEntity task : allTasks) {
            if (task.status == TaskEntity.STATUS_ACTIVE) {
                activeCount++;
                if (task.dueTime != null && task.dueTime < currentTime) {
                    overdueCount++;
                }
            } else if (task.status == TaskEntity.STATUS_COMPLETED) {
                completedCount++;
            }
        }

        tvActiveTasks.setText(String.valueOf(activeCount));
        tvCompletedTasks.setText(String.valueOf(completedCount));
        tvOverdueTasks.setText(String.valueOf(overdueCount));
    }

    private void loadTasks() {
        if (currentUserId == null) {
            Log.e(TAG, "User ID is null");
            return;
        }

        taskRepository.getAllTasks(currentUserId).observe(getViewLifecycleOwner(), new Observer<List<TaskEntity>>() {
            @Override
            public void onChanged(List<TaskEntity> tasks) {
                Log.d(TAG, "Tasks loaded: " + (tasks != null ? tasks.size() : 0));

                if (tasks != null) {
                    allTasks.clear();
                    allTasks.addAll(tasks);
                    filterTasks();
                } else {
                    updateEmptyState(true);
                }
            }
        });
    }

    private void openTaskDetails(TaskEntity task) {
        if (getActivity() != null) {
            Bundle args = new Bundle();
            args.putLong("task_id", task.id);

            TaskDetailsFragment detailsFragment = new TaskDetailsFragment();
            detailsFragment.setArguments(args);

            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, detailsFragment)
                    .addToBackStack(null)
                    .commit();
        }
    }

    private void completeTask(TaskEntity task) {
        taskRepository.completeTask(task.id, currentUserId, new TaskRepository.OnTaskCompletedCallback() {
            @Override
            public void onSuccess(int xpEarned, int newLevel) {
                Log.d(TAG, "Task completed successfully. XP earned: " + xpEarned);
                // Tasks will be automatically updated via Observer
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error completing task: " + error);
            }
        });
    }

    private void showTaskActionsDialog(TaskEntity task) {
        // Create action dialog with options: Edit, Pause, Cancel, Delete
        String[] actions = {"Izmeni", "Pauziraj", "Otkaži", "Obriši"};

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getContext());
        builder.setTitle("Opcije zadatka")
                .setItems(actions, (dialog, which) -> {
                    switch (which) {
                        case 0: // Edit
                            editTask(task);
                            break;
                        case 1: // Pause
                            pauseTask(task);
                            break;
                        case 2: // Cancel
                            cancelTask(task);
                            break;
                        case 3: // Delete
                            deleteTask(task);
                            break;
                    }
                });
        builder.show();
    }

    private void editTask(TaskEntity task) {
        // Open edit task fragment
        Bundle args = new Bundle();
        args.putLong("task_id", task.id);

        CreateTaskFragment editFragment = new CreateTaskFragment();
        editFragment.setArguments(args);

        if (getActivity() != null) {
            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, editFragment)
                    .addToBackStack(null)
                    .commit();
        }
    }

    private void pauseTask(TaskEntity task) {
        task.pause();
        taskRepository.updateTask(task);
    }

    private void cancelTask(TaskEntity task) {
        task.markCanceled();
        taskRepository.updateTask(task);
    }

    private void deleteTask(TaskEntity task) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getContext());
        builder.setTitle("Obriši zadatak")
                .setMessage("Da li ste sigurni da želite da obrišete ovaj zadatak?")
                .setPositiveButton("Obriši", (dialog, which) -> {
                    taskRepository.deleteTask(task);
                })
                .setNegativeButton("Otkaži", null)
                .show();
    }

    private String getCurrentUserId() {
        try {
            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                return FirebaseAuth.getInstance().getCurrentUser().getUid();
            }

            if (getContext() != null) {
                SharedPreferences prefs = getContext().getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
                return prefs.getString(Constants.PREF_USER_ID, null);
            }

            return null;
        } catch (Exception e) {
            Log.e(TAG, "Error getting user ID", e);
            return null;
        }
    }
}