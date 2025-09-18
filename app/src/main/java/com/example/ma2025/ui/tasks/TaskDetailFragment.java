package com.example.ma2025.ui.tasks;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.example.ma2025.R;
import com.example.ma2025.data.database.entities.TaskEntity;
import com.example.ma2025.data.database.entities.CategoryEntity;
import com.example.ma2025.viewmodels.TaskListViewModel;
import com.example.ma2025.viewmodels.CreateTaskViewModel;
import com.example.ma2025.utils.DateUtils;
import com.google.android.material.appbar.MaterialToolbar;

public class TaskDetailFragment extends Fragment {

    private static final String TAG = "TaskDetailFragment";
    private static final String ARG_TASK_ID = "task_id";

    // UI Components
    private MaterialToolbar toolbar;
    private CardView cardTask;
    private ImageView ivCategoryIndicator;
    private TextView tvTaskTitle, tvTaskDescription, tvCategoryName, tvDueDate, tvCreatedDate;
    private TextView tvDifficulty, tvImportance, tvXpValue, tvStatus;
    private CardView cardRepeatingInfo, cardProgressInfo, cardActionButtons;
    private View llOverdueWarning;
    private TextView tvRepeatInterval, tvRepeatUnit, tvStartDate, tvEndDate;
    private ProgressBar progressBar;
    private TextView tvProgressText;
    private ImageButton btnComplete, btnFail, btnPause, btnResume, btnCancel;
    private Button btnDelete, btnEdit;
    private View statusIndicator;

    // ViewModels
    private TaskListViewModel taskViewModel;
    private CreateTaskViewModel createTaskViewModel;

    // Data
    private long taskId;
    private TaskEntity currentTask;
    private CategoryEntity taskCategory;

    public static TaskDetailFragment newInstance(long taskId) {
        TaskDetailFragment fragment = new TaskDetailFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_TASK_ID, taskId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            taskId = getArguments().getLong(ARG_TASK_ID);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_task_detail, container, false);

        initViews(view);
        setupViewModels();
        setupActionButtons();
        observeData();
        loadTaskDetails();

        return view;
    }

    private void initViews(View view) {
        toolbar = view.findViewById(R.id.toolbar);
        cardTask = view.findViewById(R.id.card_task);
        ivCategoryIndicator = view.findViewById(R.id.iv_category_indicator);
        tvTaskTitle = view.findViewById(R.id.tv_task_title);
        tvTaskDescription = view.findViewById(R.id.tv_task_description);
        tvCategoryName = view.findViewById(R.id.tv_category_name);
        tvDueDate = view.findViewById(R.id.tv_due_date);
        tvCreatedDate = view.findViewById(R.id.tv_created_date);
        tvDifficulty = view.findViewById(R.id.tv_difficulty);
        tvImportance = view.findViewById(R.id.tv_importance);
        tvXpValue = view.findViewById(R.id.tv_xp_value);
        tvStatus = view.findViewById(R.id.tv_status);
        statusIndicator = view.findViewById(R.id.status_indicator);

        // Repeating task info
        cardRepeatingInfo = view.findViewById(R.id.card_repeating_info);
        tvRepeatInterval = view.findViewById(R.id.tv_repeat_interval);
        tvRepeatUnit = view.findViewById(R.id.tv_repeat_unit);
        tvStartDate = view.findViewById(R.id.tv_start_date);
        tvEndDate = view.findViewById(R.id.tv_end_date);

        // Progress info
        cardProgressInfo = view.findViewById(R.id.card_progress_info);
        progressBar = view.findViewById(R.id.progress_bar);
        tvProgressText = view.findViewById(R.id.tv_progress_text);

        // Action buttons
        cardActionButtons = view.findViewById(R.id.card_action_buttons);
        btnComplete = view.findViewById(R.id.btn_complete);
        btnFail = view.findViewById(R.id.btn_fail);
        btnPause = view.findViewById(R.id.btn_pause);
        btnResume = view.findViewById(R.id.btn_resume);
        btnCancel = view.findViewById(R.id.btn_cancel);
        btnEdit = view.findViewById(R.id.btn_edit);
        btnDelete = view.findViewById(R.id.btn_delete);

        // Warning
        llOverdueWarning = view.findViewById(R.id.ll_overdue_warning);
    }

    private void setupViewModels() {
        taskViewModel = new ViewModelProvider(this).get(TaskListViewModel.class);
        createTaskViewModel = new ViewModelProvider(this).get(CreateTaskViewModel.class);
    }

    private void setupActionButtons() {
        btnComplete.setOnClickListener(v -> {
            if (currentTask != null && currentTask.status == TaskEntity.STATUS_ACTIVE) {
                taskViewModel.completeTask(currentTask.id);
            }
        });

        btnFail.setOnClickListener(v -> {
            if (currentTask != null && currentTask.status == TaskEntity.STATUS_ACTIVE) {
                taskViewModel.failTask(currentTask.id);
            }
        });

        btnPause.setOnClickListener(v -> {
            if (currentTask != null && currentTask.status == TaskEntity.STATUS_ACTIVE) {
                taskViewModel.pauseTask(currentTask.id);
            }
        });

        btnResume.setOnClickListener(v -> {
            if (currentTask != null && currentTask.status == TaskEntity.STATUS_PAUSED) {
                taskViewModel.resumeTask(currentTask.id);
            }
        });

        btnCancel.setOnClickListener(v -> {
            if (currentTask != null &&
                    (currentTask.status == TaskEntity.STATUS_ACTIVE || currentTask.status == TaskEntity.STATUS_PAUSED)) {
                taskViewModel.cancelTask(currentTask.id);
            }
        });

        btnDelete.setOnClickListener(v -> {
            if (currentTask != null) {
                if (currentTask.isRepeating) {
                    showDeleteRecurringTaskDialog();
                } else {
                    taskViewModel.deleteTask(currentTask);
                    Toast.makeText(requireContext(), "Zadatak obrisan", Toast.LENGTH_SHORT).show();
                    navigateBack();
                }
            }
        });

        btnEdit.setOnClickListener(v -> {
            if (currentTask != null) {
                CreateTaskFragment editFragment = CreateTaskFragment.newInstanceForEdit(currentTask.id);
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, editFragment)
                        .addToBackStack(null)
                        .commit();
            }
        });
    }

    private void showDeleteRecurringTaskDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireContext());
        builder.setTitle("Brisanje ponavljajućeg zadatka")
                .setMessage("Brisanje ovog zadatka će obrisati i sva buduća ponavljanja. " +
                        "Prethodno završeni zadaci će ostati u kalendaru. Da li želite da nastavite?")
                .setPositiveButton("Obriši sve", (dialog, which) -> {
                    taskViewModel.deleteRecurringTask(currentTask.id);
                    Toast.makeText(requireContext(), "Ponavljajući zadatak i buduća ponavljanja obrisani", Toast.LENGTH_SHORT).show();
                    navigateBack();
                })
                .setNegativeButton("Otkaži", null)
                .show();
    }

    private void navigateBack() {
        if (getParentFragmentManager().getBackStackEntryCount() > 0) {
            getParentFragmentManager().popBackStack();
        } else {
            requireActivity().onBackPressed();
        }
    }

    private void observeData() {
        // Observe task completion result
        taskViewModel.getTaskCompletionResult().observe(getViewLifecycleOwner(), result -> {
            if (result != null) {
                if (result.isSuccess()) {
                    Toast.makeText(requireContext(), result.getMessage(), Toast.LENGTH_SHORT).show();
                    // Refresh task data
                    loadTaskDetails();
                } else {
                    Toast.makeText(requireContext(), "Greška: " + result.getErrorMessage(), Toast.LENGTH_SHORT).show();
                }
                taskViewModel.clearResult();
            }
        });
    }

    private void loadTaskDetails() {
        if (taskId <= 0) {
            Log.e(TAG, "Invalid task ID");
            return;
        }

        taskViewModel.getTaskById(taskId).observe(getViewLifecycleOwner(), task -> {
            if (task != null) {
                currentTask = task;
                displayTaskDetails(task);
                if (task.categoryId != null) {
                    loadCategoryInfo(task.categoryId);
                }
            } else {
                Log.e(TAG, "Task not found with ID: " + taskId);
                Toast.makeText(requireContext(), "Zadatak nije pronađen", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadCategoryInfo(Long categoryId) {
        if (categoryId != null) {
            createTaskViewModel.getCategoryById(categoryId).observe(getViewLifecycleOwner(), category -> {
                if (category != null) {
                    taskCategory = category;
                    displayCategoryInfo(category);
                }
            });
        }
    }

    private void displayTaskDetails(TaskEntity task) {
        // Basic info
        tvTaskTitle.setText(task.title);

        // Description
        if (task.description != null && !task.description.trim().isEmpty()) {
            tvTaskDescription.setText(task.description);
            tvTaskDescription.setVisibility(View.VISIBLE);
        } else {
            tvTaskDescription.setVisibility(View.GONE);
        }

        // Dates
        if (task.dueTime != null && task.dueTime > 0) {
            tvDueDate.setText("Rok: " + DateUtils.formatDateTime(task.dueTime));
            tvDueDate.setVisibility(View.VISIBLE);

            // Check if overdue
            if (task.dueTime < System.currentTimeMillis() && task.status == TaskEntity.STATUS_ACTIVE) {
                llOverdueWarning.setVisibility(View.VISIBLE);
            } else {
                llOverdueWarning.setVisibility(View.GONE);
            }
        } else {
            tvDueDate.setVisibility(View.GONE);
            llOverdueWarning.setVisibility(View.GONE);
        }

        tvCreatedDate.setText("Kreiran: " + DateUtils.formatDateTime(task.createdAt));

        // Difficulty and importance
        tvDifficulty.setText(getDifficultyText(task.difficulty));
        tvImportance.setText(getImportanceText(task.importance));

        // XP Value
        int xpValue = task.calculateXpValue(0);
        tvXpValue.setText(xpValue + " XP");

        // Repeating task info
        if (task.isRepeating) {
            cardRepeatingInfo.setVisibility(View.VISIBLE);
            tvRepeatInterval.setText("Interval: " + task.repeatInterval);
            tvRepeatUnit.setText("Jedinica: " + task.repeatUnit);

            if (task.startDate != null) {
                tvStartDate.setText("Početak: " + DateUtils.formatDate(task.startDate));
                tvStartDate.setVisibility(View.VISIBLE);
            } else {
                tvStartDate.setVisibility(View.GONE);
            }

            if (task.endDate != null) {
                tvEndDate.setText("Kraj: " + DateUtils.formatDate(task.endDate));
                tvEndDate.setVisibility(View.VISIBLE);
            } else {
                tvEndDate.setVisibility(View.GONE);
            }
        } else {
            cardRepeatingInfo.setVisibility(View.GONE);
        }

        // Status and appearance
        setupStatusAndAppearance(task);

        // Action buttons
        setupActionButtonsVisibility(task);

        // Progress (for future use)
        cardProgressInfo.setVisibility(View.GONE);
    }

    private void displayCategoryInfo(CategoryEntity category) {
        tvCategoryName.setText("Kategorija: " + category.name);
        tvCategoryName.setVisibility(View.VISIBLE);

        try {
            int color = android.graphics.Color.parseColor(category.color);
            ivCategoryIndicator.setColorFilter(color);
            if (currentTask == null) {
                statusIndicator.setBackgroundColor(color);
            }
            tvCategoryName.setTextColor(color);
        } catch (Exception e) {
            Log.w(TAG, "Failed to parse category color: " + category.color);
            tvCategoryName.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        }
    }

    private void setupStatusAndAppearance(TaskEntity task) {
        String statusText;
        int statusColor;
        boolean isOverdue = task.status == TaskEntity.STATUS_ACTIVE
                && task.dueTime != null
                && task.dueTime < System.currentTimeMillis();

        int effectiveStatus = isOverdue ? TaskEntity.STATUS_FAILED : task.status;

        switch (effectiveStatus) {
            case TaskEntity.STATUS_ACTIVE:
                statusText = "Aktivan";
                statusColor = ContextCompat.getColor(requireContext(), R.color.task_active);
                break;

            case TaskEntity.STATUS_COMPLETED:
                statusText = "Završen";
                statusColor = ContextCompat.getColor(requireContext(), R.color.task_completed);
                break;

            case TaskEntity.STATUS_FAILED:
                statusText = isOverdue ? "Neurađen" : "Neurađen";
                statusColor = ContextCompat.getColor(requireContext(), R.color.task_failed);
                break;

            case TaskEntity.STATUS_CANCELED:
                statusText = "Otkazan";
                statusColor = ContextCompat.getColor(requireContext(), R.color.task_canceled);
                break;

            case TaskEntity.STATUS_PAUSED:
                statusText = "Pauziran";
                statusColor = ContextCompat.getColor(requireContext(), R.color.task_paused);
                break;

            default:
                statusText = "Nepoznat";
                statusColor = ContextCompat.getColor(requireContext(), R.color.text_secondary);
        }

        tvStatus.setText("Status: " + statusText);
        tvStatus.setTextColor(statusColor);

        if (taskCategory == null) {
            statusIndicator.setBackgroundColor(statusColor);
        }
    }

    private void setupActionButtonsVisibility(TaskEntity task) {
        // Hide all buttons initially
        btnComplete.setVisibility(View.GONE);
        btnFail.setVisibility(View.GONE);
        btnPause.setVisibility(View.GONE);
        btnResume.setVisibility(View.GONE);
        btnCancel.setVisibility(View.GONE);
        btnEdit.setVisibility(View.GONE);
        btnDelete.setVisibility(View.GONE);

        int effectiveStatus = task.getEffectiveStatus();

        if (effectiveStatus == TaskEntity.STATUS_COMPLETED ||
                effectiveStatus == TaskEntity.STATUS_CANCELED) {

            cardActionButtons.setVisibility(View.GONE);
            return; // Izađi iz metode
        }

        // Prikaži sekciju za aktivne zadatke
        cardActionButtons.setVisibility(View.VISIBLE);

        // Hide all buttons initially
        btnComplete.setVisibility(View.GONE);

        switch (effectiveStatus) {
            case TaskEntity.STATUS_ACTIVE:
                btnComplete.setVisibility(View.VISIBLE);
                btnFail.setVisibility(View.VISIBLE);
                if (task.isRepeating) {
                    btnPause.setVisibility(View.VISIBLE);
                }
                btnCancel.setVisibility(View.VISIBLE);
                btnEdit.setVisibility(View.VISIBLE);
                btnDelete.setVisibility(View.VISIBLE);
                break;

            case TaskEntity.STATUS_PAUSED:
                btnResume.setVisibility(View.VISIBLE);
                btnCancel.setVisibility(View.VISIBLE);
                btnEdit.setVisibility(View.VISIBLE);
                btnDelete.setVisibility(View.VISIBLE);
                break;

            case TaskEntity.STATUS_COMPLETED:
                break;

            case TaskEntity.STATUS_FAILED:
            case TaskEntity.STATUS_CANCELED:
                break;
        }
    }

    private String getDifficultyText(int difficulty) {
        switch (difficulty) {
            case TaskEntity.DIFFICULTY_VERY_EASY:
                return "Veoma lak";
            case TaskEntity.DIFFICULTY_EASY:
                return "Lak";
            case TaskEntity.DIFFICULTY_HARD:
                return "Težak";
            case TaskEntity.DIFFICULTY_EXTREME:
                return "Ekstremno težak";
            default:
                return "Nepoznat";
        }
    }

    private String getImportanceText(int importance) {
        switch (importance) {
            case TaskEntity.IMPORTANCE_NORMAL:
                return "Normalan";
            case TaskEntity.IMPORTANCE_IMPORTANT:
                return "Važan";
            case TaskEntity.IMPORTANCE_VERY_IMPORTANT:
                return "Ekstremno važan";
            case TaskEntity.IMPORTANCE_SPECIAL:
                return "Specijalan";
            default:
                return "Nepoznat";
        }
    }
}