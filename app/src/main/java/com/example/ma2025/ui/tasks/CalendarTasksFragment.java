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
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ma2025.R;
import com.example.ma2025.data.database.entities.TaskEntity;
import com.example.ma2025.data.repositories.TaskRepository;
import com.example.ma2025.utils.Constants;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class CalendarTasksFragment extends Fragment {

    private static final String TAG = "CalendarTasksFragment";

    // Views
    private RecyclerView rvCalendarDays;
    private TextView tvCurrentMonth;
    private Button btnPreviousMonth, btnNextMonth;

    // Data
    private TaskRepository taskRepository;
    private CalendarDaysAdapter adapter;
    private String currentUserId;
    private Calendar currentCalendar;
    private SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calendar_tasks, container, false);

        initializeData();
        initViews(view);
        setupRecyclerView();
        setupListeners();
        updateCalendar();

        return view;
    }

    private void initializeData() {
        taskRepository = TaskRepository.getInstance(getContext());
        currentUserId = getCurrentUserId();
        currentCalendar = Calendar.getInstance();
    }

    private void initViews(View view) {
        rvCalendarDays = view.findViewById(R.id.rvCalendarDays);
        tvCurrentMonth = view.findViewById(R.id.tvCurrentMonth);
        btnPreviousMonth = view.findViewById(R.id.btnPreviousMonth);
        btnNextMonth = view.findViewById(R.id.btnNextMonth);
    }

    private void setupRecyclerView() {
        adapter = new CalendarDaysAdapter(getContext(), new ArrayList<>());
        adapter.setOnDayClickListener(date -> {
            // Show tasks for selected date
            showTasksForDate(date);
        });

        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 7);
        rvCalendarDays.setLayoutManager(layoutManager);
        rvCalendarDays.setAdapter(adapter);
    }

    private void setupListeners() {
        btnPreviousMonth.setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, -1);
            updateCalendar();
        });

        btnNextMonth.setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, 1);
            updateCalendar();
        });
    }

    private void updateCalendar() {
        // Update month title
        tvCurrentMonth.setText(monthFormat.format(currentCalendar.getTime()));

        // Generate calendar days
        List<CalendarDay> calendarDays = generateCalendarDays();
        adapter.updateDays(calendarDays);

        // Load tasks for the current month
        loadTasksForMonth();
    }

    private List<CalendarDay> generateCalendarDays() {
        List<CalendarDay> days = new ArrayList<>();

        Calendar cal = (Calendar) currentCalendar.clone();
        cal.set(Calendar.DAY_OF_MONTH, 1);

        // Get first day of week for the first day of month
        int firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        int startOffset = (firstDayOfWeek + 5) % 7; // Adjust for Monday start

        // Add previous month days
        Calendar prevMonth = (Calendar) cal.clone();
        prevMonth.add(Calendar.MONTH, -1);
        int daysInPrevMonth = prevMonth.getActualMaximum(Calendar.DAY_OF_MONTH);

        for (int i = startOffset - 1; i >= 0; i--) {
            CalendarDay day = new CalendarDay();
            day.dayNumber = daysInPrevMonth - i;
            day.isCurrentMonth = false;
            prevMonth.set(Calendar.DAY_OF_MONTH, day.dayNumber);
            day.date = prevMonth.getTimeInMillis();
            days.add(day);
        }

        // Add current month days
        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        for (int day = 1; day <= daysInMonth; day++) {
            CalendarDay calDay = new CalendarDay();
            calDay.dayNumber = day;
            calDay.isCurrentMonth = true;
            cal.set(Calendar.DAY_OF_MONTH, day);
            calDay.date = cal.getTimeInMillis();

            // Check if it's today
            Calendar today = Calendar.getInstance();
            if (cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                    cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)) {
                calDay.isToday = true;
            }

            days.add(calDay);
        }

        // Add next month days to fill the grid
        Calendar nextMonth = (Calendar) cal.clone();
        nextMonth.add(Calendar.MONTH, 1);
        int remainingDays = 42 - days.size(); // 6 weeks * 7 days

        for (int day = 1; day <= remainingDays; day++) {
            CalendarDay calDay = new CalendarDay();
            calDay.dayNumber = day;
            calDay.isCurrentMonth = false;
            nextMonth.set(Calendar.DAY_OF_MONTH, day);
            calDay.date = nextMonth.getTimeInMillis();
            days.add(calDay);
        }

        return days;
    }

    private void loadTasksForMonth() {
        if (currentUserId == null) return;

        // Get start and end of month
        Calendar startOfMonth = (Calendar) currentCalendar.clone();
        startOfMonth.set(Calendar.DAY_OF_MONTH, 1);
        startOfMonth.set(Calendar.HOUR_OF_DAY, 0);
        startOfMonth.set(Calendar.MINUTE, 0);
        startOfMonth.set(Calendar.SECOND, 0);
        startOfMonth.set(Calendar.MILLISECOND, 0);

        Calendar endOfMonth = (Calendar) startOfMonth.clone();
        endOfMonth.add(Calendar.MONTH, 1);
        endOfMonth.add(Calendar.MILLISECOND, -1);

        taskRepository.getTasksForDateRange(currentUserId,
                        startOfMonth.getTimeInMillis(),
                        endOfMonth.getTimeInMillis())
                .observe(getViewLifecycleOwner(), new Observer<List<TaskEntity>>() {
                    @Override
                    public void onChanged(List<TaskEntity> tasks) {
                        Log.d(TAG, "Tasks loaded for month: " + (tasks != null ? tasks.size() : 0));
                        if (tasks != null) {
                            adapter.updateTasks(tasks);
                        }
                    }
                });
    }

    private void showTasksForDate(long date) {
        // Open a dialog or fragment showing tasks for the selected date
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(date);

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        String dateString = dateFormat.format(cal.getTime());

        // For now, just show a toast. You could open a detailed view here.
        if (getContext() != null) {
            android.widget.Toast.makeText(getContext(),
                    "Zadaci za " + dateString,
                    android.widget.Toast.LENGTH_SHORT).show();
        }

        // TODO: Open TasksForDateFragment or dialog
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

    // Calendar day data class
    public static class CalendarDay {
        public int dayNumber;
        public long date;
        public boolean isCurrentMonth = true;
        public boolean isToday = false;
        public List<TaskEntity> tasks = new ArrayList<>();

        public boolean hasTasks() {
            return tasks != null && !tasks.isEmpty();
        }

        public int getTaskCount() {
            return tasks != null ? tasks.size() : 0;
        }
    }
}