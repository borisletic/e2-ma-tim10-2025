package com.example.ma2025.ui.tasks;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ma2025.R;
import com.example.ma2025.data.database.entities.TaskEntity;
import com.example.ma2025.data.database.entities.CategoryEntity;
import com.example.ma2025.viewmodels.TaskListViewModel;
import com.example.ma2025.viewmodels.CreateTaskViewModel;
import com.example.ma2025.utils.DateUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CalendarFragment extends Fragment implements TaskAdapter.OnTaskActionListener {

    private CalendarView calendarView;
    private RecyclerView rvDayTasks;
    private LinearLayout llEmptyState;
    private TextView tvSelectedDate;
    private FloatingActionButton fabAddTask;

    private TaskListViewModel taskViewModel;
    private CreateTaskViewModel createTaskViewModel;
    private TaskAdapter taskAdapter;

    private long selectedDate = 0;
    private List<TaskEntity> allTasks = new ArrayList<>();
    private List<CategoryEntity> categories = new ArrayList<>();
    private Map<Long, List<TaskEntity>> tasksByDate = new HashMap<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calendar, container, false);

        initViews(view);
        setupViewModels();
        setupRecyclerView();
        setupCalendar();
        setupFab();
        observeData();

        return view;
    }

    private void initViews(View view) {
        calendarView = view.findViewById(R.id.calendar_view);
        rvDayTasks = view.findViewById(R.id.rv_day_tasks);
        llEmptyState = view.findViewById(R.id.ll_empty_state);
        tvSelectedDate = view.findViewById(R.id.tv_selected_date);
        fabAddTask = view.findViewById(R.id.fab_add_task);
    }

    private void setupViewModels() {
        taskViewModel = new ViewModelProvider(this).get(TaskListViewModel.class);
        createTaskViewModel = new ViewModelProvider(this).get(CreateTaskViewModel.class);
    }

    private void setupRecyclerView() {
        taskAdapter = new CalendarTaskAdapter(requireContext(), this, categories);
        rvDayTasks.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvDayTasks.setAdapter(taskAdapter);
    }

    private void setupCalendar() {
        // Set initial selected date to today
        selectedDate = DateUtils.getStartOfDay(System.currentTimeMillis());
        updateSelectedDateText();

        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar calendar = Calendar.getInstance();
            calendar.set(year, month, dayOfMonth, 0, 0, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            selectedDate = calendar.getTimeInMillis();
            updateSelectedDateText();
            showTasksForSelectedDate();
        });

        // Set min date to show past tasks
        Calendar minDate = Calendar.getInstance();
        minDate.add(Calendar.YEAR, -1); // Show tasks from 1 year ago
        calendarView.setMinDate(minDate.getTimeInMillis());

        // Set max date to show future tasks
        Calendar maxDate = Calendar.getInstance();
        maxDate.add(Calendar.YEAR, 2); // Show tasks up to 2 years in future
        calendarView.setMaxDate(maxDate.getTimeInMillis());
    }

    private void setupFab() {
        fabAddTask.setOnClickListener(v -> {
            CreateTaskFragment createFragment = new CreateTaskFragment();

            // Pass selected date to CreateTaskFragment if needed
            Bundle args = new Bundle();
            args.putLong("selected_date", selectedDate);
            createFragment.setArguments(args);

            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, createFragment)
                    .addToBackStack(null)
                    .commit();
        });
    }

    private void observeData() {
        String userId = taskViewModel.getCurrentUserId();
        if (userId != null) {
            // Observe all tasks
            taskViewModel.getAllTasks(userId).observe(getViewLifecycleOwner(), tasks -> {
                if (tasks != null) {
                    this.allTasks = tasks;
                    Log.d("CalendarFragment", "Loaded " + tasks.size() + " tasks");
                    groupTasksByDate();
                    showTasksForSelectedDate();
                } else {
                    Log.d("CalendarFragment", "No tasks loaded");
                }
            });

            // Observe categories for color coding
            createTaskViewModel.getAllCategories().observe(getViewLifecycleOwner(), categoryList -> {
                if (categoryList != null) {
                    this.categories = categoryList;
                    if (taskAdapter instanceof CalendarTaskAdapter) {
                        ((CalendarTaskAdapter) taskAdapter).updateCategories(categoryList);
                    }
                }
            });
        }

        // Observe task completion result
        taskViewModel.getTaskCompletionResult().observe(getViewLifecycleOwner(), result -> {
            if (result != null) {
                if (result.isSuccess()) {
                    Toast.makeText(requireContext(),
                            result.getMessage(), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(),
                            "Greška: " + result.getErrorMessage(), Toast.LENGTH_SHORT).show();
                }
                taskViewModel.clearResult();
            }
        });
    }

    private void groupTasksByDate() {
        tasksByDate.clear();

        for (TaskEntity task : allTasks) {
            long taskDate = getTaskDisplayDate(task);
            if (taskDate > 0) {
                long dayStart = DateUtils.getStartOfDay(taskDate);

                if (!tasksByDate.containsKey(dayStart)) {
                    tasksByDate.put(dayStart, new ArrayList<>());
                }
                tasksByDate.get(dayStart).add(task);
            }
        }
    }

    private long getTaskDisplayDate(TaskEntity task) {
        if (task.dueTime != null && task.dueTime > 0) {
            return task.dueTime;
        } else if (task.startDate != null && task.startDate > 0) {
            return task.startDate;
        } else if (task.status == TaskEntity.STATUS_COMPLETED && task.updatedAt > 0) {
            return task.updatedAt;
        }
        return 0;
    }

    private void showTasksForSelectedDate() {
        List<TaskEntity> dayTasks = tasksByDate.get(selectedDate);

        if (dayTasks == null || dayTasks.isEmpty()) {
            // Show empty state
            rvDayTasks.setVisibility(View.GONE);
            llEmptyState.setVisibility(View.VISIBLE);
        } else {
            // Show tasks for the day
            rvDayTasks.setVisibility(View.VISIBLE);
            llEmptyState.setVisibility(View.GONE);
            taskAdapter.updateTasks(dayTasks);
        }
    }

    private void updateSelectedDateText() {
        String dateText;
        if (DateUtils.isToday(selectedDate)) {
            dateText = "Danas - " + DateUtils.formatDate(selectedDate);
        } else if (DateUtils.isYesterday(selectedDate)) {
            dateText = "Juče - " + DateUtils.formatDate(selectedDate);
        } else {
            dateText = DateUtils.formatDate(selectedDate);
        }

        tvSelectedDate.setText(dateText);
    }

    // TaskAdapter.OnTaskActionListener implementation
    @Override
    public void onTaskClick(TaskEntity task) {
        // Navigate to task details
        TaskDetailFragment detailFragment = new TaskDetailFragment();
        Bundle args = new Bundle();
        args.putLong("task_id", task.id);
        detailFragment.setArguments(args);

        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, detailFragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onTaskComplete(TaskEntity task) {
        if (task.status == TaskEntity.STATUS_ACTIVE) {
            taskViewModel.completeTask(task.id);
        }
    }

    @Override
    public void onTaskFail(TaskEntity task) {
        if (task.status == TaskEntity.STATUS_ACTIVE) {
            taskViewModel.failTask(task.id);
        }
    }

    @Override
    public void onTaskDelete(TaskEntity task) {
        taskViewModel.deleteTask(task);
        Toast.makeText(requireContext(), "Zadatak obrisan", Toast.LENGTH_SHORT).show();
    }

    // NEW: Added missing interface methods
    @Override
    public void onTaskPause(TaskEntity task) {
        if (task.status == TaskEntity.STATUS_ACTIVE) {
            taskViewModel.pauseTask(task.id);
        }
    }

    @Override
    public void onTaskResume(TaskEntity task) {
        if (task.status == TaskEntity.STATUS_PAUSED) {
            taskViewModel.resumeTask(task.id);
        }
    }

    @Override
    public void onTaskCancel(TaskEntity task) {
        if (task.status == TaskEntity.STATUS_ACTIVE || task.status == TaskEntity.STATUS_PAUSED) {
            taskViewModel.cancelTask(task.id);
        }
    }

    // Custom adapter for calendar tasks with category colors
    private static class CalendarTaskAdapter extends TaskAdapter {
        private List<CategoryEntity> categories;
        private Map<Long, String> categoryColors;

        public CalendarTaskAdapter(android.content.Context context, OnTaskActionListener listener, List<CategoryEntity> categories) {
            super(context, listener);
            this.categories = categories;
            updateCategoryColors();
        }

        public void updateCategories(List<CategoryEntity> categories) {
            this.categories = categories;
            updateCategoryColors();
            notifyDataSetChanged();
        }

        private void updateCategoryColors() {
            categoryColors = new HashMap<>();
            if (categories != null) {
                for (CategoryEntity category : categories) {
                    categoryColors.put(category.id, category.color);
                }
            }
        }

        @Override
        public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
            super.onBindViewHolder(holder, position);

            TaskEntity task = getTaskAtPosition(position);
            if (task != null && task.categoryId != null && categoryColors.containsKey(task.categoryId)) {
                String colorHex = categoryColors.get(task.categoryId);
                try {
                    int color = android.graphics.Color.parseColor(colorHex);
                    holder.itemView.findViewById(R.id.status_indicator).setBackgroundColor(color);
                } catch (Exception e) {
                    // Use default color if parsing fails
                    Log.w("CalendarTaskAdapter", "Failed to parse color: " + colorHex);
                }
            }
        }
    }
}