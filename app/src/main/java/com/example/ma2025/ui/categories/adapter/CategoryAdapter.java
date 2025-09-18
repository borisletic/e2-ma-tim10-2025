package com.example.ma2025.ui.categories.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ma2025.R;
import com.example.ma2025.data.database.entities.CategoryEntity;
import java.util.ArrayList;
import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

    private Context context;
    private List<CategoryEntity> categories;
    private OnCategoryActionListener listener;

    public interface OnCategoryActionListener {
        void onCategoryClick(CategoryEntity category);
        void onCategoryEdit(CategoryEntity category);
        void onCategoryDelete(CategoryEntity category);
    }

    public CategoryAdapter(Context context, OnCategoryActionListener listener) {
        this.context = context;
        this.categories = new ArrayList<>();
        this.listener = listener;
    }

    public void updateCategories(List<CategoryEntity> newCategories) {
        this.categories.clear();
        if (newCategories != null) {
            this.categories.addAll(newCategories);
        }
        notifyDataSetChanged();
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
        return categories.size();
    }

    public class CategoryViewHolder extends RecyclerView.ViewHolder {

        private CardView cardView;
        private TextView tvCategoryName;
        private ImageButton btnEdit, btnDelete;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);

            cardView = itemView.findViewById(R.id.card_category);
            tvCategoryName = itemView.findViewById(R.id.tv_category_name);
            btnEdit = itemView.findViewById(R.id.btn_edit);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }

        public void bind(CategoryEntity category) {
            if (category == null) return;

            tvCategoryName.setText(category.name);

            try {
                int colorInt = Color.parseColor(category.color);
                tvCategoryName.setTextColor(colorInt);
            } catch (Exception e) {
                tvCategoryName.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.text_primary));
            }

            cardView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCategoryClick(category);
                }
            });

            btnEdit.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCategoryEdit(category);
                }
            });

            btnDelete.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCategoryDelete(category);
                }
            });
        }
    }
}