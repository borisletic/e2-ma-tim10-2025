package com.example.ma2025.ui.tasks;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ma2025.R;
import com.example.ma2025.data.database.entities.TaskEntity;
import com.example.ma2025.data.repositories.CategoryRepository;
import com.example.ma2025.utils.DateUtils;
import java.util.ArrayList;
import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    protected Context context;
    protected List<TaskEntity> tasks;
    protected OnTaskActionListener listener;
    private CategoryRepository categoryRepository;

    public interface OnTaskActionListener {
        void onTaskClick(TaskEntity task);
        void onTaskComplete(TaskEntity task);
        void onTaskFail(TaskEntity task);
        void onTaskDelete(TaskEntity task);
        void onTaskPause(TaskEntity task);
        void onTaskResume(TaskEntity task);
        void onTaskCancel(TaskEntity task);
    }

    public TaskAdapter(Context context, OnTaskActionListener listener) {
        this.context = context;
        this.tasks = new ArrayList<>();
        this.listener = listener;
        this.categoryRepository = CategoryRepository.getInstance(context);
    }

    public void updateTasks(List<TaskEntity> newTasks) {
        this.tasks.clear();
        if (newTasks != null) {
            this.tasks.addAll(newTasks);
        }
        notifyDataSetChanged();
    }

    // NEW: Method for accessing tasks from subclasses
    protected TaskEntity getTaskAtPosition(int position) {
        if (position >= 0 && position < tasks.size()) {
            return tasks.get(position);
        }
        return null;
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
        return tasks.size();
    }

    public class TaskViewHolder extends RecyclerView.ViewHolder {

        private CardView cardView;
        private TextView tvTitle, tvDescription, tvCategory, tvDueDate, tvXpValue, tvStatus;
        private ImageButton btnComplete, btnFail, btnPause, btnResume, btnCancel, btnDelete;
        private View statusIndicator;
        private ImageView ivCategoryIndicator;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);

            cardView = itemView.findViewById(R.id.card_task);
            tvTitle = itemView.findViewById(R.id.tv_task_title);
            tvDescription = itemView.findViewById(R.id.tv_task_description);
            tvCategory = itemView.findViewById(R.id.tv_task_category);
            tvDueDate = itemView.findViewById(R.id.tv_due_date);
            tvXpValue = itemView.findViewById(R.id.tv_xp_value);
            tvStatus = itemView.findViewById(R.id.tv_status);
            statusIndicator = itemView.findViewById(R.id.status_indicator);
            ivCategoryIndicator = itemView.findViewById(R.id.iv_category_indicator);

            btnComplete = itemView.findViewById(R.id.btn_complete);
            btnFail = itemView.findViewById(R.id.btn_fail);
            btnPause = itemView.findViewById(R.id.btn_pause);
            btnResume = itemView.findViewById(R.id.btn_resume);
            btnCancel = itemView.findViewById(R.id.btn_cancel);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }

        public void bind(TaskEntity task) {
            if (task == null) return;

            // Basic info
            tvTitle.setText(task.title);

            // Description
            if (task.description != null && !task.description.trim().isEmpty()) {
                tvDescription.setText(task.description);
                tvDescription.setVisibility(View.VISIBLE);
            } else {
                tvDescription.setVisibility(View.GONE);
            }

            if (task.categoryId != null) {
                loadAndDisplayCategory(task.categoryId);
            } else {
                tvCategory.setVisibility(View.GONE);
            }

            // Due date
            if (task.dueTime != null && task.dueTime > 0) {
                if (DateUtils.isToday(task.dueTime)) {
                    tvDueDate.setText("Danas " + DateUtils.formatTime(task.dueTime));
                } else {
                    tvDueDate.setText(DateUtils.formatDateTime(task.dueTime));
                }
                tvDueDate.setVisibility(View.VISIBLE);
            } else if (task.isRepeating) {
                tvDueDate.setText("Ponavljajući zadatak");
                tvDueDate.setVisibility(View.VISIBLE);
            } else {
                tvDueDate.setVisibility(View.GONE);
            }

            // XP Value
            int xpValue = task.calculateXpValue(0); // Using level 0 for now
            tvXpValue.setText(xpValue + " XP");

            // Status and appearance
            setupStatusAndAppearance(task);

            // Action buttons
            setupActionButtons(task);

            // Click listeners
            cardView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTaskClick(task);
                }
            });
        }

        private void loadAndDisplayCategory(Long categoryId) {
            categoryRepository.getCategoryById(categoryId.longValue()).observeForever(category -> {
                if (category != null) {
                    tvCategory.setText("Kategorija: " + category.name);
                    tvCategory.setVisibility(View.VISIBLE);

                    try {
                        int color = android.graphics.Color.parseColor(category.color);
                        tvCategory.setTextColor(color);
                        ivCategoryIndicator.setColorFilter(color); // Ovo ste tražili

                    } catch (Exception e) {
                        tvCategory.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
                    }
                } else {
                    tvCategory.setVisibility(View.GONE);
                }
            });
        }

        private void setupStatusAndAppearance(TaskEntity task) {
            String statusText;
            int statusColor;
            int cardBackgroundColor;

            switch (task.status) {
                case TaskEntity.STATUS_ACTIVE:
                    statusText = "Aktivno";
                    statusColor = ContextCompat.getColor(context, R.color.task_active);
                    cardBackgroundColor = ContextCompat.getColor(context, R.color.white);

                    // Check if overdue
                    if (task.dueTime != null && task.dueTime < System.currentTimeMillis()) {
                        statusText = "Prošao rok";
                        statusColor = ContextCompat.getColor(context, R.color.task_overdue);
                    }
                    break;

                case TaskEntity.STATUS_COMPLETED:
                    statusText = "Urađen";
                    statusColor = ContextCompat.getColor(context, R.color.task_completed);
                    cardBackgroundColor = ContextCompat.getColor(context, R.color.background_secondary);
                    break;

                case TaskEntity.STATUS_FAILED:
                    statusText = "Neurađen";
                    statusColor = ContextCompat.getColor(context, R.color.task_failed);
                    cardBackgroundColor = ContextCompat.getColor(context, R.color.background_secondary);
                    break;

                case TaskEntity.STATUS_CANCELED:
                    statusText = "Otkazan";
                    statusColor = ContextCompat.getColor(context, R.color.text_secondary);
                    cardBackgroundColor = ContextCompat.getColor(context, R.color.background_secondary);
                    break;

                case TaskEntity.STATUS_PAUSED:
                    statusText = "Pauziran";
                    statusColor = ContextCompat.getColor(context, R.color.warning_color);
                    cardBackgroundColor = ContextCompat.getColor(context, R.color.white);
                    break;

                default:
                    statusText = "Nepoznato";
                    statusColor = ContextCompat.getColor(context, R.color.text_secondary);
                    cardBackgroundColor = ContextCompat.getColor(context, R.color.white);
            }

            tvStatus.setText(statusText);
            tvStatus.setTextColor(statusColor);
            statusIndicator.setBackgroundColor(statusColor);
            cardView.setCardBackgroundColor(cardBackgroundColor);
        }

        private void setupActionButtons(TaskEntity task) {
            btnComplete.setVisibility(View.GONE);
            btnFail.setVisibility(View.GONE);
            btnPause.setVisibility(View.GONE);
            btnResume.setVisibility(View.GONE);
            btnCancel.setVisibility(View.GONE);
            btnDelete.setVisibility(View.GONE);

            switch (task.status) {
                case TaskEntity.STATUS_ACTIVE:
                    btnComplete.setVisibility(View.VISIBLE);
                    btnFail.setVisibility(View.VISIBLE);
                    btnCancel.setVisibility(View.VISIBLE);
                    btnDelete.setVisibility(View.VISIBLE);

                    if (task.isRepeating) {
                        btnPause.setVisibility(View.VISIBLE);
                    }

                    btnComplete.setOnClickListener(v -> {
                        if (listener != null) {
                            listener.onTaskComplete(task);
                        }
                    });

                    btnFail.setOnClickListener(v -> {
                        if (listener != null) {
                            listener.onTaskFail(task);
                        }
                    });

                    btnPause.setOnClickListener(v -> {
                        if (listener != null) {
                            listener.onTaskPause(task);
                        }
                    });

                    btnCancel.setOnClickListener(v -> {
                        if (listener != null) {
                            listener.onTaskCancel(task);
                        }
                    });
                    break;

                case TaskEntity.STATUS_PAUSED:
                    btnResume.setVisibility(View.VISIBLE);
                    btnCancel.setVisibility(View.VISIBLE);
                    btnDelete.setVisibility(View.VISIBLE);

                    btnResume.setOnClickListener(v -> {
                        if (listener != null) {
                            listener.onTaskResume(task);
                        }
                    });

                    btnCancel.setOnClickListener(v -> {
                        if (listener != null) {
                            listener.onTaskCancel(task);
                        }
                    });
                    break;

                case TaskEntity.STATUS_COMPLETED:
                    break;

                case TaskEntity.STATUS_FAILED:
                case TaskEntity.STATUS_CANCELED:
                    break;

                default:
                    btnDelete.setVisibility(View.VISIBLE);
                    break;
            }

            btnDelete.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTaskDelete(task);
                }
            });
        }
    }
}