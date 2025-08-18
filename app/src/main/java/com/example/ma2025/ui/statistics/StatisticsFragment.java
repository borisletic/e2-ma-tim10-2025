// Fixed StatisticsFragment.java
package com.example.ma2025.ui.statistics;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.ma2025.R;
import com.example.ma2025.data.models.User;
import com.example.ma2025.data.preferences.PreferencesManager;
import com.example.ma2025.databinding.FragmentStatisticsBinding;
import com.example.ma2025.utils.Constants;
import com.example.ma2025.utils.DateUtils;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.*;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.*;

public class StatisticsFragment extends Fragment {

    private static final String TAG = "StatisticsFragment";
    private FragmentStatisticsBinding binding;
    private FirebaseFirestore db;
    private PreferencesManager preferencesManager;
    private User currentUser;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentStatisticsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        preferencesManager = new PreferencesManager(requireContext());

        loadUserData();
        setupCharts();
        loadStatistics();
    }

    private void loadUserData() {
        String userId = preferencesManager.getUserId();
        if (userId == null) return;

        db.collection(Constants.COLLECTION_USERS)
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentUser = documentSnapshot.toObject(User.class);
                        if (currentUser != null) {
                            displayBasicStats();
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error loading user data", e));
    }

    private void displayBasicStats() {
        // Calculate active days
        int activeDays = calculateActiveDays();
        binding.tvActiveDays.setText(String.valueOf(activeDays));

        // Display basic user stats
        binding.tvTotalTasks.setText(String.valueOf(currentUser.getTotalTasksCreated()));
        binding.tvCompletedTasks.setText(String.valueOf(currentUser.getTotalTasksCompleted()));
        binding.tvLongestStreak.setText(String.valueOf(currentUser.getLongestStreak()));

        // Calculate completion rate
        double completionRate = currentUser.getTotalTasksCreated() > 0 ?
                (double) currentUser.getTotalTasksCompleted() / currentUser.getTotalTasksCreated() * 100 : 0;
        binding.tvCompletionRate.setText(String.format("%.1f%%", completionRate));
    }

    private int calculateActiveDays() {
        if (currentUser == null) return 0;
        long daysDiff = DateUtils.getDaysDifference(currentUser.getRegistrationTime(), System.currentTimeMillis());
        return (int) Math.max(1, daysDiff);
    }

    private void setupCharts() {
        setupTasksDonutChart();
        setupCategoryBarChart();
        setupXpLineChart();
        setupDifficultyLineChart();
    }

    private void setupTasksDonutChart() {
        PieChart chart = binding.chartTasksDonut;

        // Sample data - in real app, load from database
        List<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(currentUser != null ? currentUser.getTotalTasksCompleted() : 0, "Završeni"));
        entries.add(new PieEntry(currentUser != null ? currentUser.getTotalTasksSkipped() : 0, "Preskočeni"));
        entries.add(new PieEntry(currentUser != null ? currentUser.getTotalTasksCanceled() : 0, "Otkazani"));

        PieDataSet dataSet = new PieDataSet(entries, "Zadaci");
        dataSet.setColors(new int[]{
                Color.parseColor("#4CAF50"), // Green for completed
                Color.parseColor("#FF9800"), // Orange for skipped
                Color.parseColor("#F44336")  // Red for canceled
        });
        dataSet.setValueTextSize(12f);

        PieData data = new PieData(dataSet);
        chart.setData(data);
        chart.setDrawHoleEnabled(true);
        chart.setHoleRadius(40f);
        chart.setTransparentCircleRadius(45f);

        Description desc = new Description();
        desc.setText("");
        chart.setDescription(desc);
        chart.invalidate();
    }

    private void setupCategoryBarChart() {
        BarChart chart = binding.chartCategoryBar;

        // Sample data - in real app, load from database
        List<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0, 10)); // Zdravlje
        entries.add(new BarEntry(1, 14)); // Učenje
        entries.add(new BarEntry(2, 8));  // Sport
        entries.add(new BarEntry(3, 6));  // Posao

        BarDataSet dataSet = new BarDataSet(entries, "Zadaci po kategoriji");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setValueTextSize(12f);

        BarData data = new BarData(dataSet);
        chart.setData(data);

        XAxis xAxis = chart.getXAxis();

        // FIXED: Use proper ValueFormatter class instead of lambda
        xAxis.setValueFormatter(new ValueFormatter() {
            private final String[] categories = {"Zdravlje", "Učenje", "Sport", "Posao"};

            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                return index >= 0 && index < categories.length ? categories[index] : "";
            }
        });

        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);

        Description desc = new Description();
        desc.setText("");
        chart.setDescription(desc);
        chart.invalidate();
    }

    private void setupXpLineChart() {
        LineChart chart = binding.chartXpLine;

        // Sample data for last 7 days
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            entries.add(new Entry(i, (float) (Math.random() * 50 + 10))); // Random XP values
        }

        LineDataSet dataSet = new LineDataSet(entries, "XP poslednja 7 dana");
        dataSet.setColor(Color.parseColor("#2196F3"));
        dataSet.setValueTextSize(10f);
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(4f);

        LineData data = new LineData(dataSet);
        chart.setData(data);

        XAxis xAxis = chart.getXAxis();

        // FIXED: Use proper ValueFormatter class instead of lambda
        xAxis.setValueFormatter(new ValueFormatter() {
            private final String[] days = {"Pon", "Uto", "Sre", "Čet", "Pet", "Sub", "Ned"};

            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                return index >= 0 && index < days.length ? days[index] : "";
            }
        });

        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);

        Description desc = new Description();
        desc.setText("");
        chart.setDescription(desc);
        chart.invalidate();
    }

    private void setupDifficultyLineChart() {
        LineChart chart = binding.chartDifficultyLine;

        // Sample data for average difficulty over time
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            entries.add(new Entry(i, (float) (Math.random() * 10 + 5))); // Random difficulty values
        }

        LineDataSet dataSet = new LineDataSet(entries, "Prosečna težina zadataka");
        dataSet.setColor(Color.parseColor("#FF5722"));
        dataSet.setValueTextSize(8f);
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(3f);

        LineData data = new LineData(dataSet);
        chart.setData(data);

        Description desc = new Description();
        desc.setText("");
        chart.setDescription(desc);
        chart.invalidate();
    }

    private void loadStatistics() {
        // In a real implementation, you would load actual statistics from the database
        // For now, we'll use sample data and the current user data

        if (currentUser != null) {
            // Load special missions count
            loadSpecialMissionsCount();
        }
    }

    private void loadSpecialMissionsCount() {
        String userId = preferencesManager.getUserId();
        if (userId == null) return;

        // Query for special missions where user participated
        db.collection(Constants.COLLECTION_MISSIONS)
                .whereArrayContains("participantIds", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int totalMissions = queryDocumentSnapshots.size();
                    int completedMissions = 0;

                    // Count completed missions
                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                        Boolean isCompleted = doc.getBoolean("isCompleted");
                        if (Boolean.TRUE.equals(isCompleted)) {
                            completedMissions++;
                        }
                    }

                    binding.tvSpecialMissionsStarted.setText(String.valueOf(totalMissions));
                    binding.tvSpecialMissionsCompleted.setText(String.valueOf(completedMissions));
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading special missions", e);
                    binding.tvSpecialMissionsStarted.setText("0");
                    binding.tvSpecialMissionsCompleted.setText("0");
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}