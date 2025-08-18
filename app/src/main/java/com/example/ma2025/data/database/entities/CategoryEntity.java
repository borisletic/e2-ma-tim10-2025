package com.example.ma2025.data.database.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;
import androidx.room.Index;
import androidx.annotation.NonNull;

@Entity(
        tableName = "categories",
        indices = {@Index("user_id")}
)
public class CategoryEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "user_id")
    @NonNull
    public String userId;

    @ColumnInfo(name = "name")
    public String name;

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
            "#607D8B"  // Blue Grey
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