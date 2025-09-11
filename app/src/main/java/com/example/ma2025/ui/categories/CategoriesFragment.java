package com.example.ma2025.ui.categories;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.ma2025.R;

/**
 * Fragment for Categories management
 * This is a placeholder until Student 2 implements the real functionality
 */
public class CategoriesFragment extends Fragment {

    private static final String TAG = "CategoriesFragment";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Using placeholder layout - Student 2 should replace this with actual categories layout
        return inflater.inflate(R.layout.fragment_placeholder, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // TODO: Student 2 - Implement categories management functionality here
        // This should include:
        // - Creating new task categories
        // - Editing existing categories
        // - Deleting categories
        // - Setting category colors
        // - Category statistics
    }
}