package com.example.ma2025.ui.categories;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ma2025.R;
import com.example.ma2025.data.database.entities.CategoryEntity;
import com.example.ma2025.ui.categories.adapter.CategoryAdapter;
import com.example.ma2025.viewmodels.CreateTaskViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class CategoriesFragment extends Fragment implements CategoryAdapter.OnCategoryActionListener {

    private static final String TAG = "CategoriesFragment";

    private RecyclerView recyclerView;
    private CreateTaskViewModel viewModel;
    private FloatingActionButton fabAddCategory;
    private View emptyStateView;
    private CategoryAdapter categoryAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_categories, container, false);

        setupViews(view);
        setupViewModel();
        loadCategories();

        return view;
    }

    private void setupViews(View view) {
        recyclerView = view.findViewById(R.id.rv_categories);
        fabAddCategory = view.findViewById(R.id.fab_add_category);
        emptyStateView = view.findViewById(R.id.empty_state);

        categoryAdapter = new CategoryAdapter(requireContext(), this);
        recyclerView.setAdapter(categoryAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        fabAddCategory.setOnClickListener(v -> {
            CreateCategoryDialogFragment dialog = CreateCategoryDialogFragment.newInstance();
            dialog.show(getParentFragmentManager(), "CreateCategoryDialog");
        });
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(CreateTaskViewModel.class);
    }

    private void loadCategories() {
        viewModel.getAllCategories().observe(getViewLifecycleOwner(), categories -> {
            if (categories != null) {
                android.util.Log.d(TAG, "Loaded " + categories.size() + " categories");

                categoryAdapter.updateCategories(categories);

                View headerCount = getView().findViewById(R.id.tv_categories_count);
                if (headerCount instanceof android.widget.TextView) {
                    String countText = categories.size() + " kategorija";
                    ((android.widget.TextView) headerCount).setText(countText);
                }

                if (categories.isEmpty()) {
                    recyclerView.setVisibility(View.GONE);
                    emptyStateView.setVisibility(View.VISIBLE);
                } else {
                    recyclerView.setVisibility(View.VISIBLE);
                    emptyStateView.setVisibility(View.GONE);
                }
            }
        });
    }

    @Override
    public void onCategoryClick(CategoryEntity category) {
        Toast.makeText(requireContext(), "Kliknuto na: " + category.name, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCategoryEdit(CategoryEntity category) {
        EditCategoryDialogFragment dialog = EditCategoryDialogFragment.newInstance(category);
        dialog.show(getParentFragmentManager(), "EditCategoryDialog");
    }

    @Override
    public void onCategoryDelete(CategoryEntity category) {
        androidx.appcompat.app.AlertDialog.Builder builder =
                new androidx.appcompat.app.AlertDialog.Builder(requireContext());

        builder.setTitle("Brisanje kategorije");
        builder.setMessage("Da li ste sigurni da želite da obrišete kategoriju \"" +
                category.name + "\"?\n\nOva akcija je nepovratna.");

        builder.setPositiveButton("Obriši", (dialog, which) -> {
            if (viewModel != null) {
                viewModel.deleteCategory(category);

                Toast.makeText(requireContext(), "Kategorija obrisana: " + category.name,
                        Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Otkaži", (dialog, which) -> dialog.dismiss());
        builder.show();
    }
}