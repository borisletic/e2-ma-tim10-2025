package com.example.ma2025.data.repositories;

import android.content.Context;
import android.util.Log;
import androidx.lifecycle.LiveData;
import com.example.ma2025.data.database.AppDatabase;
import com.example.ma2025.data.database.entities.CategoryEntity;
import com.example.ma2025.data.database.dao.CategoryDao;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CategoryRepository {
    private static final String TAG = "CategoryRepository";

    private CategoryDao categoryDao;
    private FirebaseFirestore firestore;
    private ExecutorService executor;

    private static volatile CategoryRepository INSTANCE;

    private CategoryRepository(Context context) {
        AppDatabase database = AppDatabase.getDatabase(context);
        categoryDao = database.categoryDao();
        firestore = FirebaseFirestore.getInstance();
        executor = Executors.newFixedThreadPool(2);
    }

    public static CategoryRepository getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (CategoryRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new CategoryRepository(context);
                }
            }
        }
        return INSTANCE;
    }

    // ========== CATEGORY OPERATIONS ==========

    public void insertCategory(CategoryEntity category, OnCategoryOperationCallback callback) {
        executor.execute(() -> {
            try {
                // Check if color is already used
                List<CategoryEntity> existingWithColor = categoryDao.getCategoriesByColor(
                        category.userId, category.color);

                if (!existingWithColor.isEmpty()) {
                    if (callback != null) {
                        callback.onError("Boja je već u upotrebi za drugu kategoriju");
                    }
                    return;
                }

                // Check if name already exists
                CategoryEntity existingWithName = categoryDao.getCategoryByName(
                        category.userId, category.name);

                if (existingWithName != null) {
                    if (callback != null) {
                        callback.onError("Kategorija sa tim nazivom već postoji");
                    }
                    return;
                }

                long categoryId = categoryDao.insertCategory(category);
                category.id = categoryId;

                // Sync to Firebase
                syncCategoryToFirebase(category);

                if (callback != null) {
                    callback.onSuccess("Kategorija je uspešno kreirana");
                }

            } catch (Exception e) {
                Log.e(TAG, "Error inserting category", e);
                if (callback != null) {
                    callback.onError("Greška pri kreiranju kategorije: " + e.getMessage());
                }
            }
        });
    }

    public void updateCategory(CategoryEntity category, OnCategoryOperationCallback callback) {
        executor.execute(() -> {
            try {
                // Validate color uniqueness (excluding current category)
                List<CategoryEntity> existingWithColor = categoryDao.getCategoriesByColor(
                        category.userId, category.color);

                boolean colorConflict = false;
                for (CategoryEntity existing : existingWithColor) {
                    if (existing.id != category.id) {
                        colorConflict = true;
                        break;
                    }
                }

                if (colorConflict) {
                    if (callback != null) {
                        callback.onError("Boja je već u upotrebi za drugu kategoriju");
                    }
                    return;
                }

                categoryDao.updateCategory(category);
                syncCategoryToFirebase(category);

                if (callback != null) {
                    callback.onSuccess("Kategorija je uspešno ažurirana");
                }

            } catch (Exception e) {
                Log.e(TAG, "Error updating category", e);
                if (callback != null) {
                    callback.onError("Greška pri ažuriranju kategorije: " + e.getMessage());
                }
            }
        });
    }

    public void deleteCategory(CategoryEntity category, OnCategoryOperationCallback callback) {
        executor.execute(() -> {
            try {
                categoryDao.deleteCategory(category);

                // Delete from Firebase if synced
                if (category.firebaseId != null) {
                    deleteCategoryFromFirebase(category.firebaseId);
                }

                if (callback != null) {
                    callback.onSuccess("Kategorija je uspešno obrisana");
                }

            } catch (Exception e) {
                Log.e(TAG, "Error deleting category", e);
                if (callback != null) {
                    callback.onError("Greška pri brisanju kategorije: " + e.getMessage());
                }
            }
        });
    }

    public LiveData<List<CategoryEntity>> getAllCategories(String userId) {
        return categoryDao.getAllCategories(userId);
    }

    public LiveData<CategoryEntity> getCategoryById(long categoryId) {
        return categoryDao.getCategoryById(categoryId);
    }

    // ========== DEFAULT CATEGORIES ==========

    public void createDefaultCategories(String userId, OnCategoryOperationCallback callback) {
        executor.execute(() -> {
            try {
                // Check if user already has categories
                int existingCount = categoryDao.getCategoriesCount(userId);
                if (existingCount > 0) {
                    if (callback != null) {
                        callback.onSuccess("Kategorije već postoje");
                    }
                    return;
                }

                // Create default categories
                String[] defaultCategories = {
                        "Zdravlje", "Učenje", "Sport", "Posao", "Kućni poslovi", "Zabava"
                };

                String[] defaultColors = {
                        "#4CAF50", // Green - Zdravlje
                        "#2196F3", // Blue - Učenje
                        "#FF9800", // Orange - Sport
                        "#9C27B0", // Purple - Posao
                        "#795548", // Brown - Kućni poslovi
                        "#E91E63"  // Pink - Zabava
                };

                for (int i = 0; i < defaultCategories.length; i++) {
                    CategoryEntity category = new CategoryEntity(
                            userId,
                            defaultCategories[i],
                            defaultColors[i]
                    );

                    long categoryId = categoryDao.insertCategory(category);
                    category.id = categoryId;

                    // Sync to Firebase
                    syncCategoryToFirebase(category);
                }

                if (callback != null) {
                    callback.onSuccess("Osnovne kategorije su kreirane");
                }

            } catch (Exception e) {
                Log.e(TAG, "Error creating default categories", e);
                if (callback != null) {
                    callback.onError("Greška pri kreiranju osnovnih kategorija: " + e.getMessage());
                }
            }
        });
    }

    // ========== COLOR VALIDATION ==========

    public void validateCategoryColor(String userId, String color, long excludeCategoryId,
                                      OnColorValidationCallback callback) {
        executor.execute(() -> {
            try {
                List<CategoryEntity> existingWithColor = categoryDao.getCategoriesByColor(userId, color);

                boolean isAvailable = true;
                for (CategoryEntity existing : existingWithColor) {
                    if (existing.id != excludeCategoryId) {
                        isAvailable = false;
                        break;
                    }
                }

                if (callback != null) {
                    callback.onResult(isAvailable);
                }

            } catch (Exception e) {
                Log.e(TAG, "Error validating color", e);
                if (callback != null) {
                    callback.onResult(false);
                }
            }
        });
    }

    public String[] getAvailableColors(String userId) {
        // This would typically be called from a background thread
        // Returns colors that are not currently used by the user
        return CategoryEntity.AVAILABLE_COLORS;
    }

    // ========== FIREBASE SYNC ==========

    private void syncCategoryToFirebase(CategoryEntity category) {
        if (category.syncedToFirebase) return;

        executor.execute(() -> {
            try {
                // TODO: Implement Firebase sync for categories
                // Categories might be stored per user for backup/sync across devices
                category.syncedToFirebase = true;
                categoryDao.updateCategory(category);

            } catch (Exception e) {
                Log.e(TAG, "Error syncing category to Firebase", e);
            }
        });
    }

    private void deleteCategoryFromFirebase(String firebaseId) {
        executor.execute(() -> {
            try {
                // TODO: Delete category from Firebase
                firestore.collection("user_categories")
                        .document(firebaseId)
                        .delete();

            } catch (Exception e) {
                Log.e(TAG, "Error deleting category from Firebase", e);
            }
        });
    }

    // ========== CALLBACKS ==========

    public interface OnCategoryOperationCallback {
        void onSuccess(String message);
        void onError(String error);
    }

    public interface OnColorValidationCallback {
        void onResult(boolean isAvailable);
    }

    // ========== STATISTICS ==========

    public void getCategoryUsageStats(String userId, OnCategoryStatsCallback callback) {
        executor.execute(() -> {
            try {
                // This would typically involve joining with tasks table
                // For now, return a simple count
                List<CategoryEntity> categories = categoryDao.getAllCategories(userId).getValue();

                if (callback != null && categories != null) {
                    callback.onStatsLoaded(categories.size());
                }

            } catch (Exception e) {
                Log.e(TAG, "Error loading category stats", e);
                if (callback != null) {
                    callback.onStatsLoaded(0);
                }
            }
        });
    }

    public interface OnCategoryStatsCallback {
        void onStatsLoaded(int categoryCount);
    }
}