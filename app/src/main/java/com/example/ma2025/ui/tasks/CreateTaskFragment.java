package com.example.ma2025.ui.tasks;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;

import com.example.ma2025.R;
import com.example.ma2025.data.database.entities.TaskEntity;
import com.example.ma2025.data.database.entities.CategoryEntity;
import com.example.ma2025.data.repositories.TaskRepository;
import com.example.ma2025.data.repositories.CategoryRepository;
import com.example.ma2025.ui.categories.CategoriesFragment;
import com.example.ma2025.utils.Constants;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class CreateTaskFragment extends Fragment {

    private static final String TAG = "CreateTaskFragment";

    // Views
    private EditText etTaskName, etTaskDescription, etRepeatInterval;
    private Spinner spinnerCategory, spinnerDifficulty, spinnerImportance, spinnerRepeatUnit;
    private Switch switchRepeating;
    private TextView tvXpValue;
    private MaterialButton btnStartDate, btnEndDate, btnExecutionTime, btnSaveTask, btnManageCategories;
    private View layoutRepeating;

    // Data
    private TaskRepository taskRepository;
    private CategoryRepository categoryRepository;
    private String currentUserId;
    private List<CategoryEntity> categories;
    private Calendar startDate, endDate, executionTime;
    private SimpleDateFormat dateFormat, timeFormat;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView called");

        View view = inflater.inflate(R.layout.fragment_create_task, container, false);

        try {
            initializeComponents(view);
            Log.d(TAG, "Fragment successfully initialized");
            return view;
        } catch (Exception e) {
            Log.e(TAG, "Error creating view", e);
            if (getContext() != null) {
                Toast.makeText(getContext(), "Greška pri inicijalizaciji: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
            return view;
        }
    }

    private void initializeComponents(View view) {
        // Verify context
        if (getContext() == null) {
            throw new IllegalStateException("Fragment context is null");
        }

        // Initialize repositories
        taskRepository = TaskRepository.getInstance(getContext());
        categoryRepository = CategoryRepository.getInstance(getContext());
        Log.d(TAG, "Repositories initialized");

        // Get current user
        currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            Log.e(TAG, "Current user ID is null");
            Toast.makeText(getContext(), "Greška: Korisnik nije prijavljen", Toast.LENGTH_SHORT).show();
            return;
        }
        Log.d(TAG, "Current user ID: " + currentUserId);

        // Initialize date/time
        startDate = Calendar.getInstance();
        endDate = Calendar.getInstance();
        endDate.add(Calendar.DAY_OF_MONTH, 7); // Default end date 7 days from now
        executionTime = Calendar.getInstance();
        dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        Log.d(TAG, "Date/time components initialized");

        // Initialize views and setup
        initViews(view);
        setupSpinners();
        setupListeners();
        loadCategories();
        updateXpValue();
    }

    private void initViews(View view) {
        Log.d(TAG, "Initializing views...");

        try {
            etTaskName = view.findViewById(R.id.etTaskName);
            etTaskDescription = view.findViewById(R.id.etTaskDescription);
            etRepeatInterval = view.findViewById(R.id.etRepeatInterval);

            spinnerCategory = view.findViewById(R.id.spinnerCategory);
            spinnerDifficulty = view.findViewById(R.id.spinnerDifficulty);
            spinnerImportance = view.findViewById(R.id.spinnerImportance);
            spinnerRepeatUnit = view.findViewById(R.id.spinnerRepeatUnit);

            switchRepeating = view.findViewById(R.id.switchRepeating);
            layoutRepeating = view.findViewById(R.id.layoutRepeating);

            btnStartDate = view.findViewById(R.id.btnStartDate);
            btnEndDate = view.findViewById(R.id.btnEndDate);
            btnExecutionTime = view.findViewById(R.id.btnExecutionTime);
            btnSaveTask = view.findViewById(R.id.btnSaveTask);
            btnManageCategories = view.findViewById(R.id.btnManageCategories);

            tvXpValue = view.findViewById(R.id.tvXpValue);

            // Verify critical views are found
            if (etTaskName == null) {
                throw new IllegalStateException("etTaskName not found in layout");
            }
            if (btnSaveTask == null) {
                throw new IllegalStateException("btnSaveTask not found in layout");
            }
            if (spinnerDifficulty == null) {
                throw new IllegalStateException("spinnerDifficulty not found in layout");
            }
            if (spinnerImportance == null) {
                throw new IllegalStateException("spinnerImportance not found in layout");
            }
            if (btnManageCategories == null) {
                throw new IllegalStateException("btnManageCategories not found in layout");
            }

            Log.d(TAG, "All views found successfully");

        } catch (Exception e) {
            Log.e(TAG, "Error finding views in layout", e);
            throw e;
        }
    }

    private void setupSpinners() {
        Log.d(TAG, "Setting up spinners...");

        try {
            // Difficulty spinner
            String[] difficulties = {
                    "Veoma lak (1 XP)",
                    "Lak (3 XP)",
                    "Težak (7 XP)",
                    "Ekstremno težak (20 XP)"
            };
            ArrayAdapter<String> difficultyAdapter = new ArrayAdapter<>(getContext(),
                    android.R.layout.simple_spinner_item, difficulties);
            difficultyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerDifficulty.setAdapter(difficultyAdapter);
            Log.d(TAG, "Difficulty spinner set up");

            // Importance spinner
            String[] importances = {
                    "Normalan (1 XP)",
                    "Važan (3 XP)",
                    "Ekstremno važan (10 XP)",
                    "Specijalan (100 XP)"
            };
            ArrayAdapter<String> importanceAdapter = new ArrayAdapter<>(getContext(),
                    android.R.layout.simple_spinner_item, importances);
            importanceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerImportance.setAdapter(importanceAdapter);
            Log.d(TAG, "Importance spinner set up");

            // Repeat unit spinner
            String[] repeatUnits = {"Dana", "Nedelja"};
            ArrayAdapter<String> repeatAdapter = new ArrayAdapter<>(getContext(),
                    android.R.layout.simple_spinner_item, repeatUnits);
            repeatAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerRepeatUnit.setAdapter(repeatAdapter);
            Log.d(TAG, "Repeat unit spinner set up");

        } catch (Exception e) {
            Log.e(TAG, "Error setting up spinners", e);
            if (getContext() != null) {
                Toast.makeText(getContext(), "Greška pri postavljanju spinera", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setupListeners() {
        Log.d(TAG, "Setting up listeners...");

        try {
            // Switch listener
            switchRepeating.setOnCheckedChangeListener((buttonView, isChecked) -> {
                Log.d(TAG, "Switch toggled: " + isChecked);
                layoutRepeating.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                updateDateButtonTexts();
            });

            // Date/time button listeners
            btnStartDate.setOnClickListener(v -> {
                Log.d(TAG, "Start date button clicked");
                showDatePicker(true);
            });

            btnEndDate.setOnClickListener(v -> {
                Log.d(TAG, "End date button clicked");
                showDatePicker(false);
            });

            btnExecutionTime.setOnClickListener(v -> {
                Log.d(TAG, "Execution time button clicked");
                showTimePicker();
            });

            // Categories management button
            btnManageCategories.setOnClickListener(v -> {
                Log.d(TAG, "Manage categories button clicked");
                openCategoriesFragment();
            });

            // Spinner listeners for XP calculation
            spinnerDifficulty.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                    Log.d(TAG, "Difficulty selected: position " + position);
                    updateXpValue();
                }
                @Override
                public void onNothingSelected(android.widget.AdapterView<?> parent) {}
            });

            spinnerImportance.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                    Log.d(TAG, "Importance selected: position " + position);
                    updateXpValue();
                }
                @Override
                public void onNothingSelected(android.widget.AdapterView<?> parent) {}
            });

            // Save button
            btnSaveTask.setOnClickListener(v -> {
                Log.d(TAG, "Save button clicked");
                saveTask();
            });

            updateDateButtonTexts();
            Log.d(TAG, "All listeners set up successfully");

        } catch (Exception e) {
            Log.e(TAG, "Error setting up listeners", e);
            if (getContext() != null) {
                Toast.makeText(getContext(), "Greška pri postavljanju dugmića", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openCategoriesFragment() {
        try {
            Log.d(TAG, "Opening categories fragment - START");

            if (getActivity() != null) {
                Log.d(TAG, "Activity is not null");

                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new CategoriesFragment())
                        .addToBackStack(null)
                        .commit();

                Log.d(TAG, "Categories fragment transaction committed");
            } else {
                Log.e(TAG, "Activity is null");
                Toast.makeText(getContext(), "Greška pri otvaranju kategorija", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error opening categories fragment", e);
            if (getContext() != null) {
                Toast.makeText(getContext(), "Greška: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loadCategories() {
        Log.d(TAG, "Loading categories for user: " + currentUserId);

        try {
            categoryRepository.getAllCategories(currentUserId).observe(getViewLifecycleOwner(), new Observer<List<CategoryEntity>>() {
                @Override
                public void onChanged(List<CategoryEntity> categoryEntities) {
                    Log.d(TAG, "Categories observer triggered, count: " + (categoryEntities != null ? categoryEntities.size() : 0));

                    if (categoryEntities != null && !categoryEntities.isEmpty()) {
                        categories = categoryEntities;
                        setupCategorySpinner();
                        Log.d(TAG, "Categories loaded successfully: " + categoryEntities.size());
                    } else {
                        Log.d(TAG, "No categories found, creating default ones");
                        createDefaultCategories();
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error setting up category observer", e);
            if (getContext() != null) {
                Toast.makeText(getContext(), "Greška pri učitavanju kategorija", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void createDefaultCategories() {
        Log.d(TAG, "Creating default categories...");

        categoryRepository.createDefaultCategories(currentUserId, new CategoryRepository.OnCategoryOperationCallback() {
            @Override
            public void onSuccess(String message) {
                Log.d(TAG, "Default categories created successfully: " + message);
                // Categories will be loaded automatically via observer
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error creating default categories: " + error);
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Greška pri kreiranju kategorija: " + error, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void setupCategorySpinner() {
        Log.d(TAG, "Setting up category spinner...");

        try {
            if (categories == null || categories.isEmpty()) {
                Log.w(TAG, "Categories list is empty");
                return;
            }

            String[] categoryNames = new String[categories.size()];
            for (int i = 0; i < categories.size(); i++) {
                categoryNames[i] = categories.get(i).name;
                Log.d(TAG, "Category " + i + ": " + categoryNames[i]);
            }

            ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(getContext(),
                    android.R.layout.simple_spinner_item, categoryNames);
            categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerCategory.setAdapter(categoryAdapter);

            Log.d(TAG, "Category spinner set up with " + categoryNames.length + " categories");

        } catch (Exception e) {
            Log.e(TAG, "Error setting up category spinner", e);
            if (getContext() != null) {
                Toast.makeText(getContext(), "Greška pri postavljanju kategorija", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showDatePicker(boolean isStartDate) {
        Log.d(TAG, "Showing date picker for " + (isStartDate ? "start" : "end") + " date");

        try {
            if (getContext() == null) {
                Log.e(TAG, "Context is null, cannot show date picker");
                return;
            }

            Calendar cal = isStartDate ? startDate : endDate;

            DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(),
                    (view, year, month, dayOfMonth) -> {
                        cal.set(year, month, dayOfMonth);
                        updateDateButtonTexts();
                        Log.d(TAG, "Date selected: " + dateFormat.format(cal.getTime()));
                    },
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH));

            datePickerDialog.show();
            Log.d(TAG, "Date picker shown successfully");

        } catch (Exception e) {
            Log.e(TAG, "Error showing date picker", e);
            if (getContext() != null) {
                Toast.makeText(getContext(), "Greška pri prikazivanju kalendara", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showTimePicker() {
        Log.d(TAG, "Showing time picker");

        try {
            if (getContext() == null) {
                Log.e(TAG, "Context is null, cannot show time picker");
                return;
            }

            TimePickerDialog timePickerDialog = new TimePickerDialog(getContext(),
                    (view, hourOfDay, minute) -> {
                        executionTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        executionTime.set(Calendar.MINUTE, minute);
                        updateDateButtonTexts();
                        Log.d(TAG, "Time selected: " + timeFormat.format(executionTime.getTime()));
                    },
                    executionTime.get(Calendar.HOUR_OF_DAY),
                    executionTime.get(Calendar.MINUTE),
                    true);

            timePickerDialog.show();
            Log.d(TAG, "Time picker shown successfully");

        } catch (Exception e) {
            Log.e(TAG, "Error showing time picker", e);
            if (getContext() != null) {
                Toast.makeText(getContext(), "Greška pri prikazivanju sata", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void updateDateButtonTexts() {
        try {
            if (switchRepeating.isChecked()) {
                btnStartDate.setText("Početak: " + dateFormat.format(startDate.getTime()));
                btnEndDate.setText("Kraj: " + dateFormat.format(endDate.getTime()));
            } else {
                btnStartDate.setText("Datum: " + dateFormat.format(startDate.getTime()));
                btnEndDate.setText("Kraj: opciono");
            }
            btnExecutionTime.setText("Vreme: " + timeFormat.format(executionTime.getTime()));

        } catch (Exception e) {
            Log.e(TAG, "Error updating date button texts", e);
        }
    }

    private void updateXpValue() {
        try {
            int difficultyXp = getDifficultyXp(spinnerDifficulty.getSelectedItemPosition());
            int importanceXp = getImportanceXp(spinnerImportance.getSelectedItemPosition());
            int totalXp = difficultyXp + importanceXp;

            tvXpValue.setText("Ukupno XP: " + totalXp);
            Log.d(TAG, "XP value updated: " + totalXp);

        } catch (Exception e) {
            Log.e(TAG, "Error updating XP value", e);
        }
    }

    private int getDifficultyXp(int position) {
        switch (position) {
            case 0: return 1;   // Very easy
            case 1: return 3;   // Easy
            case 2: return 7;   // Hard
            case 3: return 20;  // Extreme
            default: return 1;
        }
    }

    private int getImportanceXp(int position) {
        switch (position) {
            case 0: return 1;   // Normal
            case 1: return 3;   // Important
            case 2: return 10;  // Very important
            case 3: return 100; // Special
            default: return 1;
        }
    }

    private void saveTask() {
        Log.d(TAG, "Starting task save process");

        try {
            if (!validateInput()) {
                Log.w(TAG, "Input validation failed");
                return;
            }

            // Create TaskEntity
            TaskEntity task = new TaskEntity();
            task.userId = currentUserId;
            task.title = etTaskName.getText().toString().trim();
            task.description = etTaskDescription.getText().toString().trim();

            // Category
            if (categories != null && spinnerCategory.getSelectedItemPosition() >= 0
                    && spinnerCategory.getSelectedItemPosition() < categories.size()) {
                task.categoryId = categories.get(spinnerCategory.getSelectedItemPosition()).id;
                Log.d(TAG, "Category ID set: " + task.categoryId);
            }

            // Difficulty and importance (1-based to match constants)
            task.difficulty = spinnerDifficulty.getSelectedItemPosition() + 1;
            task.importance = spinnerImportance.getSelectedItemPosition() + 1;
            Log.d(TAG, "Difficulty: " + task.difficulty + ", Importance: " + task.importance);

            // Repeating setup
            task.isRepeating = switchRepeating.isChecked();

            if (task.isRepeating) {
                task.repeatInterval = Integer.parseInt(etRepeatInterval.getText().toString().trim());
                task.repeatUnit = spinnerRepeatUnit.getSelectedItemPosition() == 0 ? "day" : "week";
                task.startDate = startDate.getTimeInMillis();
                task.endDate = endDate.getTimeInMillis();
                Log.d(TAG, "Repeating task configured");
            } else {
                // For single task, combine date and time
                Calendar combined = Calendar.getInstance();
                combined.setTime(startDate.getTime());
                combined.set(Calendar.HOUR_OF_DAY, executionTime.get(Calendar.HOUR_OF_DAY));
                combined.set(Calendar.MINUTE, executionTime.get(Calendar.MINUTE));
                task.startDate = combined.getTimeInMillis();
                Log.d(TAG, "Single task configured");
            }

            task.dueTime = task.startDate; // Initially same as start date

            Log.d(TAG, "Task prepared for saving: " + task.title);

            // Save task
            taskRepository.insertTask(task, new TaskRepository.OnTaskInsertedCallback() {
                @Override
                public void onSuccess(long taskId) {
                    Log.d(TAG, "Task saved successfully with ID: " + taskId);
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "Zadatak je uspešno kreiran!", Toast.LENGTH_SHORT).show();
                            clearForm();
                        });
                    }
                }

                @Override
                public void onError(String error) {
                    Log.e(TAG, "Error saving task: " + error);
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "Greška: " + error, Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Exception during task save", e);
            if (getContext() != null) {
                Toast.makeText(getContext(), "Greška pri čuvanju: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private boolean validateInput() {
        Log.d(TAG, "Validating input...");

        try {
            String taskName = etTaskName.getText().toString().trim();
            if (taskName.isEmpty()) {
                Toast.makeText(getContext(), "Unesite naziv zadatka", Toast.LENGTH_SHORT).show();
                Log.w(TAG, "Task name is empty");
                return false;
            }

            if (categories == null || categories.isEmpty()) {
                Toast.makeText(getContext(), "Kategorije se još učitavaju...", Toast.LENGTH_SHORT).show();
                Log.w(TAG, "Categories not loaded yet");
                return false;
            }

            if (switchRepeating.isChecked()) {
                String intervalStr = etRepeatInterval.getText().toString().trim();
                if (intervalStr.isEmpty()) {
                    Toast.makeText(getContext(), "Unesite interval ponavljanja", Toast.LENGTH_SHORT).show();
                    Log.w(TAG, "Repeat interval is empty");
                    return false;
                }

                try {
                    int interval = Integer.parseInt(intervalStr);
                    if (interval <= 0) {
                        Toast.makeText(getContext(), "Interval mora biti veći od 0", Toast.LENGTH_SHORT).show();
                        Log.w(TAG, "Repeat interval is <= 0");
                        return false;
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(getContext(), "Interval mora biti broj", Toast.LENGTH_SHORT).show();
                    Log.w(TAG, "Repeat interval is not a number");
                    return false;
                }

                if (endDate.before(startDate)) {
                    Toast.makeText(getContext(), "Datum završetka mora biti posle početka", Toast.LENGTH_SHORT).show();
                    Log.w(TAG, "End date is before start date");
                    return false;
                }
            }

            Log.d(TAG, "Input validation passed");
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Error during validation", e);
            if (getContext() != null) {
                Toast.makeText(getContext(), "Greška pri validaciji", Toast.LENGTH_SHORT).show();
            }
            return false;
        }
    }

    private void clearForm() {
        Log.d(TAG, "Clearing form...");

        try {
            etTaskName.setText("");
            etTaskDescription.setText("");
            etRepeatInterval.setText("1");
            switchRepeating.setChecked(false);
            layoutRepeating.setVisibility(View.GONE);

            // Reset dates to current
            startDate = Calendar.getInstance();
            endDate = Calendar.getInstance();
            endDate.add(Calendar.DAY_OF_MONTH, 7);
            executionTime = Calendar.getInstance();
            updateDateButtonTexts();

            // Reset spinners to first position
            if (spinnerCategory.getAdapter() != null) {
                spinnerCategory.setSelection(0);
            }
            spinnerDifficulty.setSelection(0);
            spinnerImportance.setSelection(0);
            spinnerRepeatUnit.setSelection(0);

            updateXpValue();

            Log.d(TAG, "Form cleared successfully");

        } catch (Exception e) {
            Log.e(TAG, "Error clearing form", e);
        }
    }

    private String getCurrentUserId() {
        try {
            // Try Firebase first
            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                String firebaseUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                Log.d(TAG, "Got user ID from Firebase: " + firebaseUserId);
                return firebaseUserId;
            }

            // Fallback to SharedPreferences
            if (getContext() != null) {
                SharedPreferences prefs = getContext().getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
                String prefUserId = prefs.getString(Constants.PREF_USER_ID, null);
                Log.d(TAG, "Got user ID from SharedPreferences: " + prefUserId);
                return prefUserId;
            }

            Log.w(TAG, "Could not get user ID - no Firebase user and no context");
            return null;

        } catch (Exception e) {
            Log.e(TAG, "Error getting current user ID", e);
            return null;
        }
    }
}