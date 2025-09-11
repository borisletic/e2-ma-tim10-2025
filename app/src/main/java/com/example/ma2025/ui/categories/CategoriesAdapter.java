package com.example.ma2025.ui.categories;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ma2025.R;
import com.example.ma2025.data.database.entities.CategoryEntity;
import com.google.android.material.button.MaterialButton;
import java.util.List;

public class CategoriesAdapter extends RecyclerView.Adapter<CategoriesAdapter.CategoryViewHolder> {

    private List<CategoryEntity> categories;
    private Context context;
    private OnCategoryActionListener listener;

    public interface OnCategoryActionListener {
        void onEditColor(CategoryEntity category);
        void onEditName(CategoryEntity category);
        void onDelete(CategoryEntity category);
    }

    public CategoriesAdapter(Context context, List<CategoryEntity> categories) {
        this.context = context;
        this.categories = categories;
    }

    public void setOnCategoryActionListener(OnCategoryActionListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_category, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        CategoryEntity category = categories.get(position);
        holder.bind(category);
    }

    @Override
    public int getItemCount() {
        return categories != null ? categories.size() : 0;
    }

    public void updateCategories(List<CategoryEntity> newCategories) {
        this.categories = newCategories;
        notifyDataSetChanged();
    }

    class CategoryViewHolder extends RecyclerView.ViewHolder {
        private View viewCategoryColor;
        private TextView tvCategoryName;
        private TextView tvTaskCount;
        private MaterialButton btnEditColor;
        private MaterialButton btnEditName;
        private MaterialButton btnDelete;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            viewCategoryColor = itemView.findViewById(R.id.viewCategoryColor);
            tvCategoryName = itemView.findViewById(R.id.tvCategoryName);
            tvTaskCount = itemView.findViewById(R.id.tvTaskCount);
            btnEditColor = itemView.findViewById(R.id.btnEditColor);
            btnEditName = itemView.findViewById(R.id.btnEditName);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }

        public void bind(CategoryEntity category) {
            // Set category name
            tvCategoryName.setText(category.name);

            // Set category color
            try {
                int color = Color.parseColor(category.color);
                GradientDrawable drawable = (GradientDrawable) viewCategoryColor.getBackground();
                if (drawable != null) {
                    drawable.setColor(color);
                } else {
                    // Fallback: create new drawable
                    GradientDrawable newDrawable = new GradientDrawable();
                    newDrawable.setShape(GradientDrawable.OVAL);
                    newDrawable.setColor(color);
                    viewCategoryColor.setBackground(newDrawable);
                }
            } catch (Exception e) {
                // Fallback color if parsing fails
                GradientDrawable drawable = new GradientDrawable();
                drawable.setShape(GradientDrawable.OVAL);
                drawable.setColor(Color.GRAY);
                viewCategoryColor.setBackground(drawable);
            }

            // TODO: Get actual task count from database
            // For now, show placeholder
            tvTaskCount.setText("0 zadataka");

            // Set click listeners
            btnEditColor.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEditColor(category);
                }
            });

            btnEditName.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEditName(category);
                }
            });

            btnDelete.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDelete(category);
                }
            });
        }
    }
}