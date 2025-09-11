package com.example.ma2025.ui.categories;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import androidx.core.content.ContextCompat;
import com.example.ma2025.R;
import java.util.List;

public class ColorGridAdapter extends BaseAdapter {

    private Context context;
    private List<String> colors;
    private String selectedColor;

    public ColorGridAdapter(Context context, List<String> colors) {
        this.context = context;
        this.colors = colors;
    }

    @Override
    public int getCount() {
        return colors != null ? colors.size() : 0;
    }

    @Override
    public String getItem(int position) {
        return colors.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View colorView;

        if (convertView == null) {
            // Create color circle view
            colorView = new FrameLayout(context);
            int size = (int) (48 * context.getResources().getDisplayMetrics().density); // 48dp in pixels
            colorView.setLayoutParams(new ViewGroup.LayoutParams(size, size));

            // Add padding for selection indicator
            colorView.setPadding(4, 4, 4, 4);
        } else {
            colorView = convertView;
        }

        String color = colors.get(position);
        setupColorView(colorView, color);

        // Set click listener
        colorView.setOnClickListener(v -> {
            selectedColor = color;
            notifyDataSetChanged(); // Refresh to show selection
        });

        return colorView;
    }

    private void setupColorView(View colorView, String colorHex) {
        try {
            // Create circular drawable
            GradientDrawable drawable = new GradientDrawable();
            drawable.setShape(GradientDrawable.OVAL);
            drawable.setColor(Color.parseColor(colorHex));

            // Add selection indicator
            if (colorHex.equals(selectedColor)) {
                // Add white border for selected color
                drawable.setStroke(6, Color.WHITE);
                // Add outer shadow/border
                colorView.setBackground(createSelectionBackground());
            } else {
                // Add subtle border for unselected colors
                drawable.setStroke(2, Color.parseColor("#E0E0E0"));
                colorView.setBackground(null);
            }

            // Set the color as foreground
            if (colorView instanceof FrameLayout) {
                FrameLayout frameLayout = (FrameLayout) colorView;
                frameLayout.removeAllViews();

                View innerCircle = new View(context);
                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                );
                params.setMargins(4, 4, 4, 4);
                innerCircle.setLayoutParams(params);
                innerCircle.setBackground(drawable);

                frameLayout.addView(innerCircle);
            }

        } catch (Exception e) {
            // Fallback to gray if color parsing fails
            GradientDrawable drawable = new GradientDrawable();
            drawable.setShape(GradientDrawable.OVAL);
            drawable.setColor(Color.GRAY);
            colorView.setBackground(drawable);
        }
    }

    private GradientDrawable createSelectionBackground() {
        GradientDrawable selectionDrawable = new GradientDrawable();
        selectionDrawable.setShape(GradientDrawable.OVAL);
        selectionDrawable.setColor(Color.TRANSPARENT);
        selectionDrawable.setStroke(4, Color.parseColor("#2196F3")); // Blue selection border
        return selectionDrawable;
    }

    public String getSelectedColor() {
        return selectedColor;
    }

    public void setSelectedColor(String color) {
        this.selectedColor = color;
        notifyDataSetChanged();
    }
}