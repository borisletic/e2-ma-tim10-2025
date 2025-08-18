package com.example.ma2025.data.database.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.lifecycle.LiveData;
import com.example.ma2025.data.database.entities.CategoryEntity;
import java.util.List;

@Dao
public interface CategoryDao {

    @Insert
    long insertCategory(CategoryEntity category);

    @Update
    void updateCategory(CategoryEntity category);

    @Delete
    void deleteCategory(CategoryEntity category);

    @Query("SELECT * FROM categories WHERE user_id = :userId ORDER BY name ASC")
    LiveData<List<CategoryEntity>> getAllCategories(String userId);

    @Query("SELECT * FROM categories WHERE id = :categoryId")
    LiveData<CategoryEntity> getCategoryById(long categoryId);

    @Query("SELECT * FROM categories WHERE user_id = :userId AND name = :name")
    CategoryEntity getCategoryByName(String userId, String name);

    @Query("SELECT * FROM categories WHERE user_id = :userId AND color = :color")
    List<CategoryEntity> getCategoriesByColor(String userId, String color);

    @Query("SELECT COUNT(*) FROM categories WHERE user_id = :userId")
    int getCategoriesCount(String userId);

    @Query("SELECT * FROM categories WHERE synced_to_firebase = 0")
    List<CategoryEntity> getUnsyncedCategories();

    @Query("UPDATE categories SET synced_to_firebase = 1, firebase_id = :firebaseId WHERE id = :categoryId")
    void markCategoryAsSynced(long categoryId, String firebaseId);

    @Query("DELETE FROM categories WHERE user_id = :userId")
    void deleteAllUserCategories(String userId);
}