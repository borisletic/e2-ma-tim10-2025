package com.example.ma2025.ui.tasks;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;

import com.example.ma2025.R;
import com.example.ma2025.data.database.entities.TaskEntity;
import com.example.ma2025.data.repositories.TaskRepository;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TaskDetailsFragment extends Fragment {

    private static final String TAG = "TaskDetailsFragment";

    // Views
    private TextView tvTaskTitle, tvTaskDescription, tvTaskStatus, tvTaskCategory;
    private TextView tvTaskDifficulty, tvTaskImportance, tvTotalXp;
    private TextView tvTaskDate, tvTaskTime, tvRepeatInfo, tvRepeatPeriod;
    private View viewCategoryColor, layoutRepeatingInfo;
    private Button btnCompleteTask, btnPauseTask, btnCancelTask, btnEditTask, btnDeleteTask;

    // Data
    private TaskRepository taskRepository;
    private TaskEntity currentTask;
    private long taskId;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_task_details, container, false);

        initializeData();
        initViews(view);
        setupListeners();
        loadTaskDetails();

        return view;
    }

    private void initializeData() {
        taskRepository = TaskRepository.getInstance(getContext());

        if (getArguments() != null) {
            taskId = getArguments().getLong("task_id", -1);
        }

        if (taskId == -1) {
            Log.e(TAG, "No task ID provided");
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        }
    }

    private void initViews(View view) {
        tvTaskTitle = view.findViewById(R.id.tvTaskTitle);
        tvTaskDescription = view.findViewById(R.id.tvTaskDescription);
        tvTaskStatus = view.findViewById(R.id.tvTaskStatus);
        tvTaskCategory = view.findViewById(R.id.tvTaskCategory);
        tvTaskDifficulty = view.findViewById(R.id.tvTaskDifficulty);
        tvTaskImportance = view.findViewById(R.id.tvTaskImportance);
        tvTotalXp = view.findViewById(R.id.tvTotalXp);
        tvTaskDate = view.findViewById(R.id.tvTaskDate);
        tvTaskTime = view.findViewById(R.id.tvTaskTime);
        tvRepeatInfo = view.findViewById(R.id.tvRepeatInfo);
        tvRepeatPeriod = view.findViewById(R.id.tvRepeatPeriod);

        viewCategoryColor = view.findViewById(R.id.viewCategoryColor);
        layoutRepeatingInfo = view.findViewById(R.id.layoutRepeatingInfo);

        btnCompleteTask = view.findViewById(R.id.btnCompleteTask);
        btnPauseTask = view.findViewById(R.id.btnPauseTask);
        btnCancelTask = view.findViewById(R.id.btnCancelTask);
        btnEditTask = view.findViewById(R.id.btnEditTask);
        btnDeleteTask = view.findViewById(R.id.btnDeleteTask);
    }

    private void setupListeners() {
        btnCompleteTask.setOnClickListener(v -> completeTask());
        btnPauseTask.setOnClickListener(v -> pauseTask());
        btnCancelTask.setOnClickListener(v -> cancelTask());
        btnEditTask.setOnClickListener(v -> editTask());
        btnDeleteTask.setOnClickListener(v -> showDeleteConfirmation());
    }

    private void loadTaskDetails() {
        taskRepository.getTaskById(taskId).observe(getViewLifecycleOwner(), new Observer<TaskEntity>() {
            @Override
            public void onChanged(TaskEntity task) {
                if (task != null) {
                    currentTask = task;
                    populateTaskDetails(task);
                } else {
                    Log.e(TAG, "Task not found with ID: " + taskId);
                    if (getActivity() != null) {
                        Toast.makeText(getContext(), "Zadatak nije pronađen", Toast.LENGTH_SHORT).show();
                        getActivity().onBackPressed();
                    }
                }
            }
        });
    }

    private void populateTaskDetails(TaskEntity task) {
        // Basic info
        tvTaskTitle.setText(task.title);

        if (task.description != null && !task.description.trim().isEmpty()) {
            tvTaskDescription.setText(task.description);
            tvTaskDescription.setVisibility(View.VISIBLE);
        } else {
            tvTaskDescription.setVisibility(View.GONE);
        }

        // Status
        updateStatusDisplay(task);

        // Category (placeholder - load actual category)
        tvTaskCategory.setText("Kategorija"); // TODO: Load actual category name

        // Difficulty
        setDifficultyDisplay(task.difficulty);

        // Importance
        setImportanceDisplay(task.importance);

        // Total XP
        int totalXp = task.calculateXpValue(1); // Using level 1 for display
        tvTotalXp.setText("Ukupno XP: " + totalXp);

        // Date and time
        if (task.dueTime != null) {
            Date dueDate = new Date(task.dueTime);
            tvTaskDate.setText(dateFormat.format(dueDate));
            tvTaskTime.setText(timeFormat.format(dueDate));
        }

        // Repeating info
        if (task.isRepeating) {
            layoutRepeatingInfo.setVisibility(View.VISIBLE);

            String repeatText = "Svakih " + task.repeatInterval + " ";
            repeatText += task.repeatUnit.equals("day") ? "dana" : "nedelja";
            tvRepeatInfo.setText(repeatText);

            if (task.startDate != null && task.endDate != null) {
                String periodText = dateFormat.format(new Date(task.startDate)) +
                        " - " + dateFormat.format(new Date(task.endDate));
                tvRepeatPeriod.setText(periodText);
            }
        } else {
            layoutRepeatingInfo.setVisibility(View.GONE);
        }

        // Update button states
        updateButtonStates(task);
    }

    private void updateStatusDisplay(TaskEntity task) {
        String statusText;
        int statusColor;

        switch (task.status) {
            case TaskEntity.STATUS_ACTIVE:
                statusText = "Aktivno";
                statusColor = getResources().getColor(R.color.success_color);
                break;
            case TaskEntity.STATUS_COMPLETED:
                statusText = "Završeno";
                statusColor = getResources().getColor(R.color.primary_color);
                break;
            case TaskEntity.STATUS_FAILED:
                statusText = "Neuspešno";
                statusColor = getResources().getColor(R.color.error_color);
                break;
            case TaskEntity.STATUS_CANCELED:
                statusText = "Otkazano";
                statusColor = getResources().getColor(R.color.warning_color);
                break;
            case TaskEntity.STATUS_PAUSED:
                statusText = "Pauzirano";
                statusColor = getResources().getColor(R.color.text_secondary);
                break;
            default:
                statusText = "Nepoznato";
                statusColor = getResources().getColor(R.color.text_secondary);
        }

        tvTaskStatus.setText(statusText);
        tvTaskStatus.setBackgroundColor(statusColor);
    }

    private void setDifficultyDisplay(int difficulty) {
        String difficultyText;
        int difficultyColor;

        switch (difficulty) {
            case TaskEntity.DIFFICULTY_VERY_EASY:
                difficultyText = "Veoma lak (1 XP)";
                difficultyColor = getResources().getColor(R.color.difficulty_very_easy);
                break;
            case TaskEntity.DIFFICULTY_EASY:
                difficultyText = "Lak (3 XP)";
                difficultyColor = getResources().getColor(R.color.difficulty_easy);
                break;
            case TaskEntity.DIFFICULTY_HARD:
                difficultyText = "Težak (7 XP)";
                difficultyColor = getResources().getColor(R.color.difficulty_hard);
                break;
            case TaskEntity.DIFFICULTY_EXTREME:
                difficultyText = "Ekstremno težak (20 XP)";
                difficultyColor = getResources().getColor(R.color.difficulty_extreme);
                break;
            default:
                difficultyText = "Nepoznato";
                difficultyColor = getResources().getColor(R.color.text_secondary);
        }

        tvTaskDifficulty.setText(difficultyText);
        tvTaskDifficulty.setTextColor(difficultyColor);
    }

    private void setImportanceDisplay(int importance) {
        String importanceText;
        int importanceColor;

        switch (importance) {
            case TaskEntity.IMPORTANCE_NORMAL:
                importanceText = "Normalan (1 XP)";
                importanceColor = getResources().getColor(R.color.importance_normal);
                break;
            case TaskEntity.IMPORTANCE_IMPORTANT:
                importanceText = "Važan (3 XP)";
                importanceColor = getResources().getColor(R.color.importance_important);
                break;
            case TaskEntity.IMPORTANCE_VERY_IMPORTANT:
                importanceText = "Ekstremno važan (10 XP)";
                importanceColor = getResources().getColor(R.color.importance_very_important);
                break;
            case TaskEntity.IMPORTANCE_SPECIAL:
                importanceText = "Specijalan (100 XP)";
                importanceColor = getResources().getColor(R.color.importance_special);
                break;
            default:
                importanceText = "Nepoznato";
                importanceColor = getResources().getColor(R.color.text_secondary);
        }

        tvTaskImportance.setText(importanceText);
        tvTaskImportance.setTextColor(importanceColor);
    }

    private void updateButtonStates(TaskEntity task) {
        boolean isActive = task.status == TaskEntity.STATUS_ACTIVE;
        boolean isCompleted = task.status == TaskEntity.STATUS_COMPLETED;

        btnCompleteTask.setEnabled(isActive);
        btnPauseTask.setEnabled(isActive);
        btnCancelTask.setEnabled(isActive);

        if (isCompleted) {
            btnCompleteTask.setText("✓ Završeno");
            btnCompleteTask.setBackgroundColor(getResources().getColor(R.color.text_secondary));
        }
    }

    private void completeTask() {
        if (currentTask == null) return;

        String userId = getCurrentUserId();
        if (userId == null) {
            Toast.makeText(getContext(), "Greška: Korisnik nije prijavljen", Toast.LENGTH_SHORT).show();
            return;
        }

        taskRepository.completeTask(currentTask.id, userId, new TaskRepository.OnTaskCompletedCallback() {
            @Override
            public void onSuccess(int xpEarned, int newLevel) {
                Log.d(TAG, "Task completed successfully. XP earned: " + xpEarned);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        String message = "Zadatak završen! Dobili ste " + xpEarned + " XP";
                        if (newLevel > 0) {
                            message += " i dostgli ste nivo " + newLevel + "!";
                        }
                        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
                    });
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error completing task: " + error);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Greška: " + error, Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    private void pauseTask() {
        if (currentTask == null) return;

        currentTask.pause();
        taskRepository.updateTask(currentTask);
        Toast.makeText(getContext(), "Zadatak je pauziran", Toast.LENGTH_SHORT).show();
    }

    private void cancelTask() {
        if (currentTask == null) return;

        new AlertDialog.Builder(getContext())
                .setTitle("Otkaži zadatak")
                .setMessage("Da li ste sigurni da želite da otkažete ovaj zadatak?")
                .setPositiveButton("Otkaži zadatak", (dialog, which) -> {
                    currentTask.markCanceled();
                    taskRepository.updateTask(currentTask);
                    Toast.makeText(getContext(), "Zadatak je otkazan", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Ne", null)
                .show();
    }

    private void editTask() {
        Bundle args = new Bundle();
        args.putLong("task_id", taskId);

        CreateTaskFragment editFragment = new CreateTaskFragment();
        editFragment.setArguments(args);

        if (getActivity() != null) {
            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, editFragment)
                    .addToBackStack(null)
                    .commit();
        }
    }

    private void showDeleteConfirmation() {
        if (currentTask == null) return;

        new AlertDialog.Builder(getContext())
                .setTitle("Obriši zadatak")
                .setMessage("Da li ste sigurni da želite da obrišete ovaj zadatak?\n\nOva akcija je nepovratna.")
                .setPositiveButton("Obriši", (dialog, which) -> {
                    taskRepository.deleteTask(currentTask);
                    Toast.makeText(getContext(), "Zadatak je obrisan", Toast.LENGTH_SHORT).show();
                    if (getActivity() != null) {
                        getActivity().onBackPressed();
                    }
                })
                .setNegativeButton("Otkaži", null)
                .show();
    }

    private String getCurrentUserId() {
        // Same implementation as in other fragments
        try {
            if (com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null) {
                return com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();
            }

            if (getContext() != null) {
                android.content.SharedPreferences prefs = getContext().getSharedPreferences(
                        com.example.ma2025.utils.Constants.PREFS_NAME, android.content.Context.MODE_PRIVATE);
                return prefs.getString(com.example.ma2025.utils.Constants.PREF_USER_ID, null);
            }

            return null;
        } catch (Exception e) {
            Log.e(TAG, "Error getting user ID", e);
            return null;
        }
    }
}