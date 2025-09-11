package com.example.ma2025.ui.tasks;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ma2025.R;
import com.example.ma2025.data.database.entities.TaskEntity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TasksListAdapter extends RecyclerView.Adapter<TasksListAdapter.TaskViewHolder> {

    private Context context;
    private List<TaskEntity> tasks;
    private OnTaskActionListener listener;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

    public interface OnTaskActionListener {
        void onTaskClick(TaskEntity task);
        void onCompleteTask(TaskEntity task);
        void onTaskAction(TaskEntity task);
    }

    public TasksListAdapter(Context context, List<TaskEntity> tasks) {
        this.context = context;
        this.tasks = tasks;
    }

    public void setOnTaskActionListener(OnTaskActionListener listener) {
        this.listener = listener;
    }

    public void updateTasks(List<TaskEntity> newTasks) {
        this.tasks = newTasks;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        TaskEntity task = tasks.get(position);
        holder.bind(task);
    }

    @Override
    public int getItemCount() {
        return tasks != null ? tasks.size() : 0;
    }

    class TaskViewHolder extends RecyclerView.ViewHolder {
        private View viewCategoryColor;
        private TextView tvTaskTitle, tvTaskStatus, tvTaskCategory;
        private TextView tvTaskDifficulty, tvTaskImportance, tvTaskXp;
        private TextView tvTaskDate, tvTaskTime, tvTaskRepeating;
        private Button btnCompleteTask, btnTaskActions;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);

            viewCategoryColor = itemView.findViewById(R.id.viewCategoryColor);
            tvTaskTitle = itemView.findViewById(R.id.tvTaskTitle);
            tvTaskStatus = itemView.findViewById(R.id.tvTaskStatus);
            tvTaskCategory = itemView.findViewById(R.id.tvTaskCategory);
            tvTaskDifficulty = itemView.findViewById(R.id.tvTaskDifficulty);
            tvTaskImportance = itemView.findViewById(R.id.tvTaskImportance);
            tvTaskXp = itemView.findViewById(R.id.tvTaskXp);
            tvTaskDate = itemView.findViewById(R.id.tvTaskDate);
            tvTaskTime = itemView.findViewById(R.id.tvTaskTime);
            tvTaskRepeating = itemView.findViewById(R.id.tvTaskRepeating);
            btnCompleteTask = itemView.findViewById(R.id.btnCompleteTask);
            btnTaskActions = itemView.findViewById(R.id.btnTaskActions);
        }

        public void bind(TaskEntity task) {
            // Task title
            tvTaskTitle.setText(task.title);

            // Task status
            setTaskStatus(task);

            // Category (placeholder - you'll need to get category info)
            tvTaskCategory.setText("Kategorija");

            // Difficulty
            setDifficulty(task.difficulty);

            // Importance
            setImportance(task.importance);

            // XP calculation
            int totalXp = calculateXp(task.difficulty, task.importance);
            tvTaskXp.setText(totalXp + " XP");

            // Date and time
            if (task.dueTime != null) {
                Date dueDate = new Date(task.dueTime);
                tvTaskDate.setText(dateFormat.format(dueDate));
                tvTaskTime.setText(timeFormat.format(dueDate));
            }

            // Repeating indicator
            if (task.isRepeating) {
                tvTaskRepeating.setVisibility(View.VISIBLE);
                tvTaskRepeating.setText("Ponavljajući");
            } else {
                tvTaskRepeating.setVisibility(View.GONE);
            }

            // Click listeners
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTaskClick(task);
                }
            });

            btnCompleteTask.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCompleteTask(task);
                }
            });

            btnTaskActions.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTaskAction(task);
                }
            });

            // Update button visibility based on task status
            updateButtonVisibility(task);
        }

        private void setTaskStatus(TaskEntity task) {
            switch (task.status) {
                case TaskEntity.STATUS_ACTIVE:
                    tvTaskStatus.setText("Aktivno");
                    tvTaskStatus.setBackgroundColor(context.getResources().getColor(R.color.success_color));
                    break;
                case TaskEntity.STATUS_COMPLETED:
                    tvTaskStatus.setText("Završeno");
                    tvTaskStatus.setBackgroundColor(context.getResources().getColor(R.color.success_color));
                    break;
                case TaskEntity.STATUS_FAILED:
                    tvTaskStatus.setText("Neuspešno");
                    tvTaskStatus.setBackgroundColor(context.getResources().getColor(R.color.error_color));
                    break;
                case TaskEntity.STATUS_CANCELED:
                    tvTaskStatus.setText("Otkazano");
                    tvTaskStatus.setBackgroundColor(context.getResources().getColor(R.color.warning_color));
                    break;
                case TaskEntity.STATUS_PAUSED:
                    tvTaskStatus.setText("Pauzirano");
                    tvTaskStatus.setBackgroundColor(context.getResources().getColor(R.color.info_color));
                    break;
                default:
                    tvTaskStatus.setText("Nepoznato");
                    tvTaskStatus.setBackgroundColor(context.getResources().getColor(R.color.text_secondary));
            }
        }

        private void setDifficulty(int difficulty) {
            switch (difficulty) {
                case TaskEntity.DIFFICULTY_VERY_EASY:
                    tvTaskDifficulty.setText("Veoma lak");
                    tvTaskDifficulty.setTextColor(context.getResources().getColor(R.color.difficulty_very_easy));
                    break;
                case TaskEntity.DIFFICULTY_EASY:
                    tvTaskDifficulty.setText("Lak");
                    tvTaskDifficulty.setTextColor(context.getResources().getColor(R.color.difficulty_easy));
                    break;
                case TaskEntity.DIFFICULTY_HARD:
                    tvTaskDifficulty.setText("Težak");
                    tvTaskDifficulty.setTextColor(context.getResources().getColor(R.color.difficulty_hard));
                    break;
                case TaskEntity.DIFFICULTY_EXTREME:
                    tvTaskDifficulty.setText("Ekstremno");
                    tvTaskDifficulty.setTextColor(context.getResources().getColor(R.color.difficulty_extreme));
                    break;
                default:
                    tvTaskDifficulty.setText("Nepoznato");
            }
        }

        private void setImportance(int importance) {
            switch (importance) {
                case TaskEntity.IMPORTANCE_NORMAL:
                    tvTaskImportance.setText("Normalan");
                    tvTaskImportance.setTextColor(context.getResources().getColor(R.color.importance_normal));
                    break;
                case TaskEntity.IMPORTANCE_IMPORTANT:
                    tvTaskImportance.setText("Važan");
                    tvTaskImportance.setTextColor(context.getResources().getColor(R.color.importance_important));
                    break;
                case TaskEntity.IMPORTANCE_VERY_IMPORTANT:
                    tvTaskImportance.setText("Ext. važan");
                    tvTaskImportance.setTextColor(context.getResources().getColor(R.color.importance_very_important));
                    break;
                case TaskEntity.IMPORTANCE_SPECIAL:
                    tvTaskImportance.setText("Specijalan");
                    tvTaskImportance.setTextColor(context.getResources().getColor(R.color.importance_special));
                    break;
                default:
                    tvTaskImportance.setText("Nepoznato");
            }
        }

        private int calculateXp(int difficulty, int importance) {
            int difficultyXp = 0;
            switch (difficulty) {
                case 1: difficultyXp = 1; break;
                case 2: difficultyXp = 3; break;
                case 3: difficultyXp = 7; break;
                case 4: difficultyXp = 20; break;
            }

            int importanceXp = 0;
            switch (importance) {
                case 1: importanceXp = 1; break;
                case 2: importanceXp = 3; break;
                case 3: importanceXp = 10; break;
                case 4: importanceXp = 100; break;
            }

            return difficultyXp + importanceXp;
        }

        private void updateButtonVisibility(TaskEntity task) {
            if (task.status == TaskEntity.STATUS_COMPLETED ||
                    task.status == TaskEntity.STATUS_CANCELED ||
                    task.status == TaskEntity.STATUS_FAILED) {
                btnCompleteTask.setVisibility(View.GONE);
            } else {
                btnCompleteTask.setVisibility(View.VISIBLE);
            }
        }
    }
}