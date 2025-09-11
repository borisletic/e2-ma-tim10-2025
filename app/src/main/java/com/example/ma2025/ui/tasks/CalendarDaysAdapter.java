package com.example.ma2025.ui.tasks;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ma2025.R;
import com.example.ma2025.data.database.entities.TaskEntity;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CalendarDaysAdapter extends RecyclerView.Adapter<CalendarDaysAdapter.DayViewHolder> {

    private Context context;
    private List<CalendarTasksFragment.CalendarDay> days;
    private Map<Long, List<TaskEntity>> tasksMap = new HashMap<>();
    private OnDayClickListener listener;

    public interface OnDayClickListener {
        void onDayClick(long date);
    }

    public CalendarDaysAdapter(Context context, List<CalendarTasksFragment.CalendarDay> days) {
        this.context = context;
        this.days = days;
    }

    public void setOnDayClickListener(OnDayClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_calendar_day, parent, false);
        return new DayViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
        CalendarTasksFragment.CalendarDay day = days.get(position);
        holder.bind(day);
    }

    @Override
    public int getItemCount() {
        return days != null ? days.size() : 0;
    }

    public void updateDays(List<CalendarTasksFragment.CalendarDay> newDays) {
        this.days = newDays;
        notifyDataSetChanged();
    }

    public void updateTasks(List<TaskEntity> tasks) {
        tasksMap.clear();

        // Group tasks by date
        for (TaskEntity task : tasks) {
            if (task.dueTime != null) {
                long dayKey = getDayKey(task.dueTime);

                if (!tasksMap.containsKey(dayKey)) {
                    tasksMap.put(dayKey, new java.util.ArrayList<>());
                }
                tasksMap.get(dayKey).add(task);
            }
        }

        // Update days with tasks
        for (CalendarTasksFragment.CalendarDay day : days) {
            long dayKey = getDayKey(day.date);
            if (tasksMap.containsKey(dayKey)) {
                day.tasks = tasksMap.get(dayKey);
            } else {
                day.tasks.clear();
            }
        }

        notifyDataSetChanged();
    }

    private long getDayKey(long timestamp) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    class DayViewHolder extends RecyclerView.ViewHolder {
        private CardView cardView;
        private TextView tvDayNumber;
        private LinearLayout layoutTasks;
        private TextView tvMoreTasks;

        public DayViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (CardView) itemView;
            tvDayNumber = itemView.findViewById(R.id.tvDayNumber);
            layoutTasks = itemView.findViewById(R.id.layoutTasks);
            tvMoreTasks = itemView.findViewById(R.id.tvMoreTasks);
        }

        public void bind(CalendarTasksFragment.CalendarDay day) {
            // Set day number
            tvDayNumber.setText(String.valueOf(day.dayNumber));

            // Style based on month and today
            updateDayAppearance(day);

            // Clear previous task indicators
            layoutTasks.removeAllViews();
            tvMoreTasks.setVisibility(View.GONE);

            // Add task indicators
            if (day.hasTasks()) {
                addTaskIndicators(day);
            }

            // Set click listener
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDayClick(day.date);
                }
            });
        }

        private void updateDayAppearance(CalendarTasksFragment.CalendarDay day) {
            // Set text color based on month
            if (day.isCurrentMonth) {
                tvDayNumber.setTextColor(context.getResources().getColor(R.color.text_primary));
                cardView.setCardBackgroundColor(context.getResources().getColor(android.R.color.white));
            } else {
                tvDayNumber.setTextColor(context.getResources().getColor(R.color.text_hint));
                cardView.setCardBackgroundColor(context.getResources().getColor(R.color.background_color));
            }

            // Highlight today
            if (day.isToday) {
                cardView.setCardBackgroundColor(context.getResources().getColor(R.color.primary_light));
                tvDayNumber.setTextColor(context.getResources().getColor(R.color.primary_color));
                tvDayNumber.setTextSize(14);
                tvDayNumber.setTypeface(null, android.graphics.Typeface.BOLD);
            } else {
                tvDayNumber.setTextSize(12);
                tvDayNumber.setTypeface(null, android.graphics.Typeface.NORMAL);
            }
        }

        private void addTaskIndicators(CalendarTasksFragment.CalendarDay day) {
            int maxVisible = 3; // Maximum task indicators to show
            int taskCount = day.getTaskCount();

            for (int i = 0; i < Math.min(taskCount, maxVisible); i++) {
                TaskEntity task = day.tasks.get(i);
                View indicator = createTaskIndicator(task);
                layoutTasks.addView(indicator);
            }

            // Show "more tasks" indicator if needed
            if (taskCount > maxVisible) {
                tvMoreTasks.setText("+" + (taskCount - maxVisible));
                tvMoreTasks.setVisibility(View.VISIBLE);
            }
        }

        private View createTaskIndicator(TaskEntity task) {
            View indicator = new View(context);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    (int) (2 * context.getResources().getDisplayMetrics().density) // 2dp height
            );
            params.setMargins(0, 1, 0, 1);
            indicator.setLayoutParams(params);

            // Set color based on task status and category
            int color = getTaskColor(task);
            indicator.setBackgroundColor(color);

            return indicator;
        }

        private int getTaskColor(TaskEntity task) {
            // Return color based on task status and category
            switch (task.status) {
                case TaskEntity.STATUS_COMPLETED:
                    return context.getResources().getColor(R.color.success_color);
                case TaskEntity.STATUS_FAILED:
                    return context.getResources().getColor(R.color.error_color);
                case TaskEntity.STATUS_CANCELED:
                    return context.getResources().getColor(R.color.warning_color);
                case TaskEntity.STATUS_PAUSED:
                    return context.getResources().getColor(R.color.text_secondary);
                case TaskEntity.STATUS_ACTIVE:
                default:
                    // TODO: Get actual category color from database
                    // For now, use importance-based colors
                    return getImportanceColor(task.importance);
            }
        }

        private int getImportanceColor(int importance) {
            switch (importance) {
                case TaskEntity.IMPORTANCE_NORMAL:
                    return context.getResources().getColor(R.color.importance_normal);
                case TaskEntity.IMPORTANCE_IMPORTANT:
                    return context.getResources().getColor(R.color.importance_important);
                case TaskEntity.IMPORTANCE_VERY_IMPORTANT:
                    return context.getResources().getColor(R.color.importance_very_important);
                case TaskEntity.IMPORTANCE_SPECIAL:
                    return context.getResources().getColor(R.color.importance_special);
                default:
                    return context.getResources().getColor(R.color.primary_color);
            }
        }
    }
}