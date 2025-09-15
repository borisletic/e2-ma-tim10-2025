package com.example.ma2025.data.database.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;
import androidx.room.Index;
import androidx.annotation.NonNull;

import java.io.Serializable;

@Entity(
        tableName = "categories",
        indices = {@Index("user_id")}
)
public class CategoryEntity implements Serializable {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "user_id")
    @NonNull
    public String userId;

    @ColumnInfo(name = "name")
    public String name;

    @Override
    public String toString() {
        return name; // ovo će se prikazivati u Spinneru
    }

    @ColumnInfo(name = "color")
    public String color; // Hex color code like "#FF5722"

    @ColumnInfo(name = "created_at")
    public long createdAt;

    @ColumnInfo(name = "updated_at")
    public long updatedAt;

    @ColumnInfo(name = "synced_to_firebase")
    public boolean syncedToFirebase;

    @ColumnInfo(name = "firebase_id")
    public String firebaseId;

    // Constructors
    public CategoryEntity() {
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
        this.syncedToFirebase = false;
    }

    public CategoryEntity(String userId, String name, String color) {
        this();
        this.userId = userId;
        this.name = name;
        this.color = color;
    }

    // Utility methods
    public void updateName(String newName) {
        this.name = newName;
        this.updatedAt = System.currentTimeMillis();
        this.syncedToFirebase = false;
    }

    public void updateColor(String newColor) {
        this.color = newColor;
        this.updatedAt = System.currentTimeMillis();
        this.syncedToFirebase = false;
    }

    // Predefined colors for categories
    public static final String[] AVAILABLE_COLORS = {
            "#F44336", // Red
            "#E91E63", // Pink
            "#9C27B0", // Purple
            "#673AB7", // Deep Purple
            "#3F51B5", // Indigo
            "#2196F3", // Blue
            "#03A9F4", // Light Blue
            "#00BCD4", // Cyan
            "#009688", // Teal
            "#4CAF50", // Green
            "#8BC34A", // Light Green
            "#CDDC39", // Lime
            "#FFEB3B", // Yellow
            "#FFC107", // Amber
            "#FF9800", // Orange
            "#FF5722", // Deep Orange
            "#795548", // Brown
            "#9E9E9E", // Grey
            "#607D8B", // Blue Grey
            "#D32F2F", // Dark Red
            "#C2185B", // Dark Pink
            "#7B1FA2", // Dark Purple
            "#512DA8", // Dark Deep Purple
            "#303F9F", // Dark Indigo
            "#1976D2", // Dark Blue
            "#0288D1", // Dark Light Blue
            "#0097A7", // Dark Cyan
            "#00796B", // Dark Teal
            "#388E3C", // Dark Green
            "#689F38", // Dark Light Green
            "#AFD135", // Dark Lime
            "#FDD835", // Dark Yellow
            "#FFB300", // Dark Amber
            "#F57C00", // Dark Orange
            "#E64A19", // Dark Deep Orange
            "#5D4037", // Dark Brown
            "#616161", // Dark Grey
            "#455A64", // Dark Blue Grey
    };

    public static boolean isValidColor(String color) {
        if (color == null || !color.startsWith("#") || color.length() != 7) {
            return false;
        }
        try {
            Integer.parseInt(color.substring(1), 16);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}