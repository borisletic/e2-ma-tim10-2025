package com.example.ma2025.ui.categories;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
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
import com.example.ma2025.ui.categories.adapter.ColorPickerAdapter;
import com.example.ma2025.viewmodels.CreateTaskViewModel;
import com.google.firebase.auth.FirebaseAuth;

public class CreateCategoryDialogFragment extends DialogFragment implements ColorPickerAdapter.OnColorSelectedListener {

    private EditText etCategoryName;
    private RecyclerView rvColorPicker;
    private Button btnSave, btnCancel;

    private ColorPickerAdapter colorPickerAdapter;
    private CreateTaskViewModel viewModel;
    private String selectedColor = null;

    public static CreateCategoryDialogFragment newInstance() {
        return new CreateCategoryDialogFragment();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_create_category, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupViewModel();
        setupColorPicker();
        setupButtons();
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
        loadAvailableColors();
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            // Postavite širinu na 90% ekrana
            WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
            params.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.9);
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            dialog.getWindow().setAttributes(params);
        }
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

            // Check if color is already used
            if (colorPickerAdapter.isColorUsed(selectedColor)) {
                Toast.makeText(requireContext(), "Izabrana boja se već koristi", Toast.LENGTH_SHORT).show();
                return;
            }

            createCategory(name, selectedColor);
        });
    }

    private void createCategory(String name, String color) {
        String userId = getCurrentUserId();
        if (userId == null) {
            Toast.makeText(requireContext(), "Greška: korisnik nije ulogovan", Toast.LENGTH_SHORT).show();
            return;
        }

        CategoryEntity category = new CategoryEntity(userId, name, color);

        // Use CategoryRepository with existing callback interface
        com.example.ma2025.data.repositories.CategoryRepository categoryRepository =
                com.example.ma2025.data.repositories.CategoryRepository.getInstance(requireContext());

        categoryRepository.insertCategory(category, new com.example.ma2025.data.repositories.CategoryRepository.OnCategoryOperationCallback() {
            @Override
            public void onSuccess(String message) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Kategorija kreirana: " + name, Toast.LENGTH_SHORT).show();
                    dismiss();
                });
            }

            @Override
            public void onError(String error) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Greška: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private String getCurrentUserId() {
        return FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
    }

    @Override
    public void onColorSelected(String color) {
        selectedColor = color;
    }
}