package com.example.ma2025.ui.statistics;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.ma2025.R;

/**
 * Fragment for Task management (Zadatak)
 * This fragment is now dedicated to Student 2's task management functionality.
 * The statistics functionality has been moved to ProfileFragment.
 */
public class StatisticsFragment extends Fragment {

    private static final String TAG = "TaskFragment";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Using placeholder layout - Student 2 should replace this with actual task management layout
        return inflater.inflate(R.layout.fragment_create_task, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // TODO: Student 2 - Implement task management functionality here
        // This should include:
        // - Task creation (jednokratni i ponavljajući zadaci)
        // - Task editing and deletion
        // - Task completion/marking
        // - Task categories management
        // - Task scheduling and reminders
    }
}