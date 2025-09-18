package com.example.ma2025.ui.categories;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ma2025.R;
import com.example.ma2025.data.database.entities.CategoryEntity;
import com.example.ma2025.data.repositories.CategoryRepository;
import com.example.ma2025.ui.categories.adapter.ColorPickerAdapter;
import com.example.ma2025.viewmodels.CreateTaskViewModel;

public class EditCategoryDialogFragment extends DialogFragment implements ColorPickerAdapter.OnColorSelectedListener {

    private static final String ARG_CATEGORY_ID = "category_id";
    private long categoryId;
    private EditText etCategoryName;
    private RecyclerView rvColorPicker;
    private Button btnSave, btnCancel, btnDelete;

    private ColorPickerAdapter colorPickerAdapter;
    private CreateTaskViewModel viewModel;
    private CategoryEntity categoryToEdit;
    private String selectedColor = null;

    public static EditCategoryDialogFragment newInstance(CategoryEntity category) {
        EditCategoryDialogFragment fragment = new EditCategoryDialogFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_CATEGORY_ID, category.id);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            categoryId = getArguments().getLong(ARG_CATEGORY_ID);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            android.view.WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
            params.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.9);
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            dialog.getWindow().setAttributes(params);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_edit_category, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupViewModel();
        setupColorPicker();
        setupButtons();
        if (categoryId != 0) {
            loadCategoryById(categoryId);
        }
    }

    private void loadCategoryById(long id) {
        viewModel.getCategoryById(id).observe(this, category -> {
            if (category != null) {
                categoryToEdit = category;
                populateFields();
            }
        });
    }

    private void initViews(View view) {
        etCategoryName = view.findViewById(R.id.et_category_name);
        rvColorPicker = view.findViewById(R.id.rv_color_picker);
        btnSave = view.findViewById(R.id.btn_save);
        btnCancel = view.findViewById(R.id.btn_cancel);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(requireActivity()).get(CreateTaskViewModel.class);
    }

    private void setupColorPicker() {
        colorPickerAdapter = new ColorPickerAdapter(this);
        rvColorPicker.setLayoutManager(new GridLayoutManager(requireContext(), 3));
        rvColorPicker.setAdapter(colorPickerAdapter);

        // Load available colors and check which ones are already used
        loadAvailableColors();
    }

    private void loadAvailableColors() {
        viewModel.getAllCategories().observe(this, categories -> {
            if (categories != null) {
                // Get list of used colors - compatible with API 21
                String[] usedColors = new String[categories.size()];
                for (int i = 0; i < categories.size(); i++) {
                    usedColors[i] = categories.get(i).color;
                }

                // Update adapter with used colors info
                colorPickerAdapter.updateUsedColors(usedColors);
            }
        });
    }

    private void populateFields() {
        if (categoryToEdit != null) {
            etCategoryName.setText(categoryToEdit.name);
            selectedColor = categoryToEdit.color;
            colorPickerAdapter.setSelectedColor(categoryToEdit.color);
        }
    }

    private void setupButtons() {
        btnCancel.setOnClickListener(v -> dismiss());

        btnSave.setOnClickListener(v -> {
            String name = etCategoryName.getText().toString().trim();

            if (name.isEmpty()) {
                Toast.makeText(requireContext(), "Unesite naziv kategorije", Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedColor == null) {
                Toast.makeText(requireContext(), "Izaberite boju kategorije", Toast.LENGTH_SHORT).show();
                return;
            }

            // Check if color is already used (excluding current category)
            if (colorPickerAdapter.isColorUsed(selectedColor) && !selectedColor.equals(categoryToEdit.color)) {
                Toast.makeText(requireContext(), "Izabrana boja se već koristi", Toast.LENGTH_SHORT).show();
                return;
            }

            updateCategory(name, selectedColor);
        });
    }

    private void updateCategory(String name, String color) {
        categoryToEdit.name = name;
        categoryToEdit.color = color;

        viewModel.updateCategory(categoryToEdit, new CategoryRepository.OnCategoryOperationCallback() {
            @Override
            public void onSuccess(String message) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                dismiss();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onColorSelected(String color) {
        selectedColor = color;
    }
}