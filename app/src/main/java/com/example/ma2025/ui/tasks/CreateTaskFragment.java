package com.example.ma2025.ui.tasks;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.example.ma2025.R;
import com.example.ma2025.data.database.entities.TaskEntity;
import com.example.ma2025.data.database.entities.CategoryEntity;
import com.example.ma2025.viewmodels.CreateTaskViewModel;
import com.example.ma2025.utils.DateUtils;
import com.example.ma2025.utils.Constants;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.util.Calendar;
import java.util.List;

public class CreateTaskFragment extends Fragment {

    private static final String ARG_EDIT_TASK_ID = "edit_task_id";

    // UI Components
    private TextInputEditText etTaskName, etTaskDescription;
    private TextInputLayout tilTaskName, tilTaskDescription;
    private Spinner spCategory, spDifficulty, spImportance;
    private RadioGroup rgTaskType;
    private RadioButton rbSingleTask, rbRepeatingTask;
    private LinearLayout llRepeatingOptions, llSingleTaskOptions;
    private Spinner spRepeatInterval, spRepeatUnit;
    private Button btnSelectDateTime, btnSelectStartDate, btnSelectEndDate;
    private Button btnCreateTask, btnCancel;
    private TextView tvSelectedDateTime, tvSelectedStartDate, tvSelectedEndDate;
    private TextView tvXpPreview;

    // Data
    private CreateTaskViewModel viewModel;
    private List<CategoryEntity> categories;
    private long selectedDateTime = 0;
    private long selectedStartDate = 0;
    private long selectedEndDate = 0;

    // Edit mode variables
    private boolean isEditMode = false;
    private long editTaskId = -1;
    private TaskEntity currentEditTask = null;

    // Spinner adapters
    private ArrayAdapter<CategoryEntity> categoryAdapter;
    private ArrayAdapter<String> difficultyAdapter, importanceAdapter, intervalAdapter, unitAdapter;

    // Arrays for spinners
    private String[] difficultyArray = {
            "Veoma lak (1 XP)", "Lak (3 XP)", "Težak (7 XP)", "Ekstremno težak (20 XP)"
    };

    private String[] importanceArray = {
            "Normalan (1 XP)", "Važan (3 XP)", "Ekstremno važan (10 XP)", "Specijalan (100 XP)"
    };

    private String[] intervalArray = {"1", "2", "3", "4", "5", "6", "7"};
    private String[] unitArray = {"Dan", "Nedelja"};

    public static CreateTaskFragment newInstance() {
        return new CreateTaskFragment();
    }

    public static CreateTaskFragment newInstanceForEdit(long taskId) {
        CreateTaskFragment fragment = new CreateTaskFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_EDIT_TASK_ID, taskId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check for edit mode
        if (getArguments() != null && getArguments().containsKey(ARG_EDIT_TASK_ID)) {
            isEditMode = true;
            editTaskId = getArguments().getLong(ARG_EDIT_TASK_ID);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_create_task, container, false);

        initViews(view);
        setupViewModel();
        setupSpinners();
        setupListeners();
        observeData();

        // Load task for editing if in edit mode
        if (isEditMode) {
            loadTaskForEditing();
        }

        return view;
    }

    private void initViews(View view) {
        // Text inputs
        etTaskName = view.findViewById(R.id.et_task_name);
        etTaskDescription = view.findViewById(R.id.et_task_description);
        tilTaskName = view.findViewById(R.id.til_task_name);
        tilTaskDescription = view.findViewById(R.id.til_task_description);
        llSingleTaskOptions = view.findViewById(R.id.ll_single_task_options);

        // Dodaj TextWatcher-e za veliko prvo slovo
        addCapitalizeFirstLetterWatcher(etTaskName);
        addCapitalizeFirstLetterWatcher(etTaskDescription);

        // Spinners
        spCategory = view.findViewById(R.id.sp_category);
        spDifficulty = view.findViewById(R.id.sp_difficulty);
        spImportance = view.findViewById(R.id.sp_importance);
        spRepeatInterval = view.findViewById(R.id.sp_repeat_interval);
        spRepeatUnit = view.findViewById(R.id.sp_repeat_unit);

        // Radio buttons for task type
        rgTaskType = view.findViewById(R.id.rg_task_type);
        rbSingleTask = view.findViewById(R.id.rb_single_task);
        rbRepeatingTask = view.findViewById(R.id.rb_repeating_task);

        // Repeating options layout
        llRepeatingOptions = view.findViewById(R.id.ll_repeating_options);

        // Buttons
        btnSelectDateTime = view.findViewById(R.id.btn_select_date_time);
        btnSelectStartDate = view.findViewById(R.id.btn_select_start_date);
        btnSelectEndDate = view.findViewById(R.id.btn_select_end_date);
        btnCreateTask = view.findViewById(R.id.btn_create_task);
        btnCancel = view.findViewById(R.id.btn_cancel);

        // TextViews
        tvSelectedDateTime = view.findViewById(R.id.tv_selected_date_time);
        tvSelectedStartDate = view.findViewById(R.id.tv_selected_start_date);
        tvSelectedEndDate = view.findViewById(R.id.tv_selected_end_date);
        tvXpPreview = view.findViewById(R.id.tv_xp_preview);

        // Update UI based on mode
        if (isEditMode) {
            btnCreateTask.setText("Sačuvaj izmene");
        } else {
            btnCreateTask.setText("Kreiraj");
            // Initially hide repeating options for new tasks
            llRepeatingOptions.setVisibility(View.GONE);
        }
    }

    // Helper metoda za dodavanje TextWatcher-a
    private void addCapitalizeFirstLetterWatcher(EditText editText) {
        editText.addTextChangedListener(new TextWatcher() {
            private boolean isEditing = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                if (isEditing) return;
                if (s.length() > 0) {
                    isEditing = true;
                    char firstChar = Character.toUpperCase(s.charAt(0));
                    String rest = s.length() > 1 ? s.subSequence(1, s.length()).toString() : "";
                    s.replace(0, s.length(), firstChar + rest);
                    isEditing = false;
                }
            }
        });
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(CreateTaskViewModel.class);
    }

    private void setupSpinners() {
        // Difficulty spinner
        difficultyAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, difficultyArray);
        difficultyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spDifficulty.setAdapter(difficultyAdapter);

        // Importance spinner
        importanceAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, importanceArray);
        importanceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spImportance.setAdapter(importanceAdapter);

        // Repeat interval spinner
        intervalAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, intervalArray);
        intervalAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spRepeatInterval.setAdapter(intervalAdapter);

        // Repeat unit spinner
        unitAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, unitArray);
        unitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spRepeatUnit.setAdapter(unitAdapter);
    }

    private void setupListeners() {
        // Task type radio button change listener
        rgTaskType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_repeating_task) {
                Log.d("CreateTaskFragment", "Switching to repeating task");
                llRepeatingOptions.setVisibility(View.VISIBLE);

                btnSelectDateTime.setVisibility(View.GONE);
                tvSelectedDateTime.setVisibility(View.GONE);
                btnSelectStartDate.setVisibility(View.VISIBLE);
                btnSelectEndDate.setVisibility(View.VISIBLE);
                tvSelectedStartDate.setVisibility(View.VISIBLE);
                tvSelectedEndDate.setVisibility(View.VISIBLE);
            } else {
                llRepeatingOptions.setVisibility(View.GONE);
                btnSelectDateTime.setVisibility(View.VISIBLE);
                tvSelectedDateTime.setVisibility(View.VISIBLE);
                btnSelectStartDate.setVisibility(View.GONE);
                btnSelectEndDate.setVisibility(View.GONE);
                tvSelectedStartDate.setVisibility(View.GONE);
                tvSelectedEndDate.setVisibility(View.GONE);
            }
        });

        // Date/Time selection buttons
        btnSelectDateTime.setOnClickListener(v -> showDateTimePicker());
        btnSelectStartDate.setOnClickListener(v -> showStartDatePicker());
        btnSelectEndDate.setOnClickListener(v -> showEndDatePicker());

        // XP preview update listeners
        spDifficulty.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateXpPreview();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spImportance.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateXpPreview();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Create/Update task button
        btnCreateTask.setOnClickListener(v -> saveTask());

        // Cancel button
        btnCancel.setOnClickListener(v -> navigateBack());
    }

    private void observeData() {
        // Observe categories
        viewModel.getAllCategories().observe(getViewLifecycleOwner(), categoryList -> {
            if (categoryList != null) {
                this.categories = categoryList;
                setupCategorySpinner();
                // Set category selection for edit mode
                if (isEditMode) {
                    populateCategorySelection();
                }
            }
        });

        // Observe task creation/update result
        viewModel.getTaskCreationResult().observe(getViewLifecycleOwner(), result -> {
            if (result != null) {
                if (result.isSuccess()) {
                    String message = isEditMode ? "Zadatak je uspešno ažuriran!" : "Zadatak je uspešno kreiran!";
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                    navigateBack();
                } else {
                    Toast.makeText(requireContext(), "Greška: " + result.getErrorMessage(), Toast.LENGTH_LONG).show();
                }
                viewModel.clearResult();
            }
        });
    }

    private void loadTaskForEditing() {
        viewModel.getTaskById(editTaskId).observe(getViewLifecycleOwner(), task -> {
            if (task != null) {
                currentEditTask = task;
                populateFormWithTask(task);
            }
        });
    }

    private void populateFormWithTask(TaskEntity task) {
        // Basic info
        etTaskName.setText(task.title);
        etTaskDescription.setText(task.description != null ? task.description : "");

        // Difficulty and importance (position = value - 1)
        spDifficulty.setSelection(task.difficulty - 1);
        spImportance.setSelection(task.importance - 1);

        // Task type
        if (task.isRepeating) {
            rbRepeatingTask.setChecked(true);
            llRepeatingOptions.setVisibility(View.VISIBLE);

            // Repeating options
            if (task.repeatInterval != null) {
                // Find position in intervalArray
                String intervalStr = String.valueOf(task.repeatInterval);
                for (int i = 0; i < intervalArray.length; i++) {
                    if (intervalArray[i].equals(intervalStr)) {
                        spRepeatInterval.setSelection(i);
                        break;
                    }
                }
            }

            if (task.repeatUnit != null) {
                // Find position in unitArray
                String unit = task.repeatUnit.toLowerCase();
                for (int i = 0; i < unitArray.length; i++) {
                    if (unitArray[i].toLowerCase().equals(unit) ||
                            (unit.equals("dan") && unitArray[i].equals("Dan")) ||
                            (unit.equals("day") && unitArray[i].equals("Dan")) ||
                            (unit.equals("nedelja") && unitArray[i].equals("Nedelja")) ||
                            (unit.equals("week") && unitArray[i].equals("Nedelja"))) {
                        spRepeatUnit.setSelection(i);
                        break;
                    }
                }
            }

            // Dates
            if (task.startDate != null) {
                selectedStartDate = task.startDate;
                tvSelectedStartDate.setText(DateUtils.formatDate(selectedStartDate));
                tvSelectedStartDate.setVisibility(View.VISIBLE);
                btnSelectStartDate.setVisibility(View.VISIBLE);
            }

            if (task.endDate != null) {
                selectedEndDate = task.endDate;
                tvSelectedEndDate.setText(DateUtils.formatDate(selectedEndDate));
                tvSelectedEndDate.setVisibility(View.VISIBLE);
                btnSelectEndDate.setVisibility(View.VISIBLE);
            }

            // Hide single task options
            btnSelectDateTime.setVisibility(View.GONE);
            tvSelectedDateTime.setVisibility(View.GONE);
        } else {
            rbSingleTask.setChecked(true);
            llRepeatingOptions.setVisibility(View.GONE);

            // Single task due time
            if (task.dueTime != null) {
                selectedDateTime = task.dueTime;
                tvSelectedDateTime.setText(DateUtils.formatDateTime(selectedDateTime));
                tvSelectedDateTime.setVisibility(View.VISIBLE);
                btnSelectDateTime.setVisibility(View.VISIBLE);
            }

            // Hide repeating task options
            btnSelectStartDate.setVisibility(View.GONE);
            btnSelectEndDate.setVisibility(View.GONE);
            tvSelectedStartDate.setVisibility(View.GONE);
            tvSelectedEndDate.setVisibility(View.GONE);
        }

        // Update XP preview
        updateXpPreview();
    }

    private void populateCategorySelection() {
        if (currentEditTask != null && currentEditTask.categoryId != null && categories != null) {
            for (int i = 0; i < categories.size(); i++) {
                if (categories.get(i).id == currentEditTask.categoryId.longValue()) {
                    spCategory.setSelection(i);
                    break;
                }
            }
        }
    }

    // DODATO: Metoda za navigaciju nazad
    private void navigateBack() {
        try {
            // Pokušaj da odeš nazad u stack
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            } else {
                // Ako nema back stack-a, vrati se na TaskListFragment
                TaskListFragment taskListFragment = new TaskListFragment();
                getParentFragmentManager().beginTransaction()
                        .replace(getCurrentContainerId(), taskListFragment)
                        .commit();
            }
        } catch (Exception e) {
            Log.e("CreateTaskFragment", "Error navigating back", e);
            // Fallback - finish Activity ako je potrebno
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        }
    }

    // DODATO: Metoda za pronalaženje container ID-ja
    private int getCurrentContainerId() {
        // Pokušaj sa uobičajenim ID-jevima
        if (getView() != null && getView().getParent() instanceof ViewGroup) {
            ViewGroup parent = (ViewGroup) getView().getParent();
            return parent.getId();
        }

        // Fallback ID-jevi
        int[] commonIds = {
                android.R.id.content,
                R.id.fragment_container
        };

        for (int id : commonIds) {
            try {
                if (getActivity() != null && getActivity().findViewById(id) != null) {
                    return id;
                }
            } catch (Exception e) {
                // Nastavi sa sledećim ID-jem
            }
        }

        return android.R.id.content; // Poslednji fallback
    }

    private void setupCategorySpinner() {
        if (categories != null && !categories.isEmpty()) {
            categoryAdapter = new CategorySpinnerAdapter(requireContext(), categories);
            spCategory.setAdapter(categoryAdapter);
        }
    }

    private void showDateTimePicker() {
        Calendar calendar = Calendar.getInstance();

        DatePickerDialog dateDialog = new DatePickerDialog(requireContext(),
                (view, year, month, dayOfMonth) -> {
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                    // After date is selected, show time picker
                    TimePickerDialog timeDialog = new TimePickerDialog(requireContext(),
                            (timeView, hourOfDay, minute) -> {
                                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                                calendar.set(Calendar.MINUTE, minute);
                                calendar.set(Calendar.SECOND, 0);

                                selectedDateTime = calendar.getTimeInMillis();
                                tvSelectedDateTime.setText(DateUtils.formatDateTime(selectedDateTime));

                            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true);
                    timeDialog.show();

                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));

        // Only set min date for new tasks, not for editing
        if (!isEditMode) {
            dateDialog.getDatePicker().setMinDate(System.currentTimeMillis());
        }
        dateDialog.show();
    }

    private void showStartDatePicker() {
        Calendar calendar = Calendar.getInstance();

        DatePickerDialog dialog = new DatePickerDialog(requireContext(),
                (view, year, month, dayOfMonth) -> {
                    calendar.set(year, month, dayOfMonth, 0, 0, 0);
                    selectedStartDate = calendar.getTimeInMillis();
                    tvSelectedStartDate.setText(DateUtils.formatDate(selectedStartDate));
                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));

        // Only set min date for new tasks, not for editing
        if (!isEditMode) {
            dialog.getDatePicker().setMinDate(System.currentTimeMillis());
        }
        dialog.show();
    }

    private void showEndDatePicker() {
        Calendar calendar = Calendar.getInstance();

        DatePickerDialog dialog = new DatePickerDialog(requireContext(),
                (view, year, month, dayOfMonth) -> {
                    calendar.set(year, month, dayOfMonth, 23, 59, 59);
                    selectedEndDate = calendar.getTimeInMillis();
                    tvSelectedEndDate.setText(DateUtils.formatDate(selectedEndDate));
                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));

        if (selectedStartDate > 0) {
            dialog.getDatePicker().setMinDate(selectedStartDate);
        } else if (!isEditMode) {
            dialog.getDatePicker().setMinDate(System.currentTimeMillis());
        }
        dialog.show();
    }

    private void updateXpPreview() {
        int difficulty = spDifficulty.getSelectedItemPosition() + 1;
        int importance = spImportance.getSelectedItemPosition() + 1;

        int difficultyXp = getDifficultyXp(difficulty);
        int importanceXp = getImportanceXp(importance);
        int totalXp = difficultyXp + importanceXp;

        tvXpPreview.setText(String.format("Ukupno XP: %d", totalXp));
    }

    private int getDifficultyXp(int difficulty) {
        switch (difficulty) {
            case 1: return Constants.XP_VERY_EASY;  // 1
            case 2: return Constants.XP_EASY;       // 3
            case 3: return Constants.XP_HARD;       // 7
            case 4: return Constants.XP_EXTREME;    // 20
            default: return Constants.XP_VERY_EASY;
        }
    }

    private int getImportanceXp(int importance) {
        switch (importance) {
            case 1: return Constants.XP_NORMAL;            // 1
            case 2: return Constants.XP_IMPORTANT;         // 3
            case 3: return Constants.XP_VERY_IMPORTANT;    // 10
            case 4: return Constants.XP_SPECIAL;           // 100
            default: return Constants.XP_NORMAL;
        }
    }

    private void saveTask() {
        if (!validateInputs()) {
            return;
        }

        if (isEditMode) {
            updateExistingTask();
        } else {
            createNewTask();
        }
    }

    private void updateExistingTask() {
        TaskEntity task = createTaskFromForm();
        task.id = editTaskId;
        // Zadržaj originalne timestamps
        if (currentEditTask != null) {
            task.createdAt = currentEditTask.createdAt;
        }

        viewModel.updateTask(task);
    }

    private void createNewTask() {
        TaskEntity task = createTaskFromForm();
        viewModel.createTask(task);
    }

    private TaskEntity createTaskFromForm() {
        String userId = viewModel.getCurrentUserId();

        TaskEntity task = new TaskEntity();
        task.userId = userId;
        task.title = etTaskName.getText().toString().trim();
        task.description = etTaskDescription.getText().toString().trim();

        // Category
        if (spCategory.getSelectedItem() != null) {
            Object selectedItem = spCategory.getSelectedItem();
            if (selectedItem instanceof CategoryEntity) {
                CategoryEntity selectedCategory = (CategoryEntity) selectedItem;
                task.categoryId = selectedCategory.id;
            }
        }

        // Difficulty and importance (pozicija + 1)
        task.difficulty = spDifficulty.getSelectedItemPosition() + 1;
        task.importance = spImportance.getSelectedItemPosition() + 1;

        // Task type and scheduling
        if (rbRepeatingTask.isChecked()) {
            task.isRepeating = true;
            task.repeatInterval = Integer.parseInt(intervalArray[spRepeatInterval.getSelectedItemPosition()]);
            task.repeatUnit = unitArray[spRepeatUnit.getSelectedItemPosition()].toLowerCase();
            task.startDate = selectedStartDate;
            task.endDate = selectedEndDate;
        } else {
            task.isRepeating = false;
            task.dueTime = selectedDateTime;
        }

        return task;
    }

    private boolean validateInputs() {
        boolean isValid = true;

        // Validate task name
        String taskName = etTaskName.getText().toString().trim();
        if (TextUtils.isEmpty(taskName)) {
            tilTaskName.setError("Unesite naziv zadatka");
            isValid = false;
        } else if (taskName.length() < Constants.MIN_TASK_TITLE_LENGTH) {
            tilTaskName.setError("Naziv mora imati najmanje " + Constants.MIN_TASK_TITLE_LENGTH + " karaktera");
            isValid = false;
        } else if (taskName.length() > Constants.MAX_TASK_TITLE_LENGTH) {
            tilTaskName.setError("Naziv može imati najviše " + Constants.MAX_TASK_TITLE_LENGTH + " karaktera");
            isValid = false;
        } else {
            tilTaskName.setError(null);
        }

        // Validate description length
        String description = etTaskDescription.getText().toString().trim();
        if (description.length() > Constants.MAX_TASK_DESCRIPTION_LENGTH) {
            tilTaskDescription.setError("Opis može imati najviše " + Constants.MAX_TASK_DESCRIPTION_LENGTH + " karaktera");
            isValid = false;
        } else {
            tilTaskDescription.setError(null);
        }

        // Validate category selection
        if (spCategory.getSelectedItem() == null) {
            Toast.makeText(requireContext(), "Izaberite kategoriju", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        // Validate date/time selection based on task type
        if (rbRepeatingTask.isChecked()) {
            if (selectedStartDate == 0) {
                Toast.makeText(requireContext(), "Izaberite datum početka", Toast.LENGTH_SHORT).show();
                isValid = false;
            }
            if (selectedEndDate == 0) {
                Toast.makeText(requireContext(), "Izaberite datum završetka", Toast.LENGTH_SHORT).show();
                isValid = false;
            }
            if (selectedStartDate > 0 && selectedEndDate > 0 && selectedStartDate >= selectedEndDate) {
                Toast.makeText(requireContext(), "Datum završetka mora biti posle datuma početka", Toast.LENGTH_SHORT).show();
                isValid = false;
            }
        } else {
            if (selectedDateTime == 0) {
                Toast.makeText(requireContext(), "Izaberite datum i vreme izvršenja", Toast.LENGTH_SHORT).show();
                isValid = false;
            }
        }

        return isValid;
    }

    private static class CategorySpinnerAdapter extends ArrayAdapter<CategoryEntity> {

        public CategorySpinnerAdapter(@NonNull Context context, List<CategoryEntity> categories) {
            super(context, 0, categories); // 0 jer koristimo custom layout
        }

        private View createView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext())
                        .inflate(R.layout.spinner_category_item, parent, false);
            }

            TextView tvName = convertView.findViewById(R.id.tv_category_name);
            View vCircle = convertView.findViewById(R.id.v_color_circle);

            CategoryEntity category = getItem(position);
            if (category != null) {
                tvName.setText(category.name);
                try {
                    int color = Color.parseColor(category.color);
                    tvName.setTextColor(color);
                    vCircle.setBackground(createCircleDrawable(color));
                } catch (Exception e) {
                    tvName.setTextColor(Color.BLACK);
                    vCircle.setBackground(createCircleDrawable(Color.BLACK));
                }
            }

            return convertView;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            return createView(position, convertView, parent);
        }

        @Override
        public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            return createView(position, convertView, parent);
        }

        private GradientDrawable createCircleDrawable(int color) {
            GradientDrawable drawable = new GradientDrawable();
            drawable.setShape(GradientDrawable.OVAL);
            drawable.setColor(color);
            return drawable;
        }
    }
}