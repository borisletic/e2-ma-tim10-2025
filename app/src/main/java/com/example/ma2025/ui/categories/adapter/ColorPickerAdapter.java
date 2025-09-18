package com.example.ma2025.ui.categories.adapter;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ma2025.R;
import com.example.ma2025.data.database.entities.CategoryEntity;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ColorPickerAdapter extends RecyclerView.Adapter<ColorPickerAdapter.ColorViewHolder> {

    private String[] availableColors;
    private Set<String> usedColors;
    private String selectedColor;
    private OnColorSelectedListener listener;

    public interface OnColorSelectedListener {
        void onColorSelected(String color);
    }

    public ColorPickerAdapter(OnColorSelectedListener listener) {
        this.listener = listener;
        this.availableColors = CategoryEntity.AVAILABLE_COLORS;
        this.usedColors = new HashSet<>();
        this.selectedColor = null;
    }

    public void setSelectedColor(String color) {
        this.selectedColor = color;
        notifyDataSetChanged();
    }

    public void updateUsedColors(String[] usedColors) {
        this.usedColors.clear();
        if (usedColors != null) {
            this.usedColors.addAll(Arrays.asList(usedColors));
        }
        notifyDataSetChanged();
    }

    public boolean isColorUsed(String color) {
        return usedColors.contains(color);
    }

    @NonNull
    @Override
    public ColorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_color_picker, parent, false);
        return new ColorViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ColorViewHolder holder, int position) {
        String color = availableColors[position];
        holder.bind(color, usedColors.contains(color), color.equals(selectedColor));
    }

    @Override
    public int getItemCount() {
        return availableColors.length;
    }

    public class ColorViewHolder extends RecyclerView.ViewHolder {
        private View colorCircle;
        private View selectionIndicator;
        private View usedIndicator;

        public ColorViewHolder(@NonNull View itemView) {
            super(itemView);
            colorCircle = itemView.findViewById(R.id.color_circle);
            selectionIndicator = itemView.findViewById(R.id.selection_indicator);
            usedIndicator = itemView.findViewById(R.id.used_indicator);
        }

        public void bind(String color, boolean isUsed, boolean isSelected) {
            // Set color
            try {
                int colorInt = Color.parseColor(color);
                ViewCompat.setBackgroundTintList(colorCircle, ColorStateList.valueOf(colorInt));
            } catch (Exception e) {
                ViewCompat.setBackgroundTintList(colorCircle, ColorStateList.valueOf(Color.GRAY));
            }

            // Show/hide selection indicator
            selectionIndicator.setVisibility(isSelected ? View.VISIBLE : View.GONE);

            // Show/hide used indicator
            usedIndicator.setVisibility(isUsed ? View.VISIBLE : View.GONE);

            // Set click listener
            itemView.setOnClickListener(v -> {
                if (!isUsed) {
                    // Update selection - ispravka ovde
                    ColorPickerAdapter.this.selectedColor = color; // Dodaj ColorPickerAdapter.this.

                    // Notify adapter to update visual state
                    ColorPickerAdapter.this.notifyDataSetChanged(); // Dodaj ColorPickerAdapter.this.

                    // Notify listener
                    if (listener != null) {
                        listener.onColorSelected(color);
                    }
                }
            });

            // Disable click if color is used
            itemView.setAlpha(isUsed ? 0.5f : 1.0f);
            itemView.setClickable(!isUsed);
        }
    }
}