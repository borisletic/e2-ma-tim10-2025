// DatabaseManager.java
package com.example.ma2025.data;

import android.content.Context;
import android.util.Log;
import androidx.lifecycle.LiveData;
import com.example.ma2025.data.repositories.TaskRepository;
import com.example.ma2025.data.repositories.CategoryRepository;
import com.example.ma2025.data.database.AppDatabase;
import com.example.ma2025.data.database.entities.TaskEntity;
import com.example.ma2025.data.database.entities.CategoryEntity;
import com.example.ma2025.data.database.entities.UserProgressEntity;
import com.example.ma2025.data.database.entities.DailyStatsEntity;
import com.example.ma2025.utils.Constants;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Central database manager that coordinates between SQLite and Firebase
 * Acts as a single point of access for all database operations
 */
public class DatabaseManager {
    private static final String TAG = "DatabaseManager";

    private Context context;
    private TaskRepository taskRepository;
    private CategoryRepository categoryRepository;
    private FirebaseFirestore firestore;
    private ExecutorService executor;

    private static volatile DatabaseManager INSTANCE;

    private DatabaseManager(Context context) {
        this.context = context.getApplicationContext();
        this.taskRepository = TaskRepository.getInstance(context);
        this.categoryRepository = CategoryRepository.getInstance(context);
        this.firestore = FirebaseFirestore.getInstance();
        this.executor = Executors.newFixedThreadPool(4);
    }

    public static DatabaseManager getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (DatabaseManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new DatabaseManager(context);
                }
            }
        }
        return INSTANCE;
    }

    // ========== USER INITIALIZATION ==========

    /**
     * Initialize user data when they first log in
     * Creates default categories and user progress entry
     */
    public void initializeUserData(String userId, OnInitializationCallback callback) {
        executor.execute(() -> {
            try {
                Log.d(TAG, "Initializing user data for: " + userId);

                // Initialize user progress
                taskRepository.initializeUserProgress(userId);

                // Create default categories
                categoryRepository.createDefaultCategories(userId, new CategoryRepository.OnCategoryOperationCallback() {
                    @Override
                    public void onSuccess(String message) {
                        Log.d(TAG, "Default categories created: " + message);
                        if (callback != null) {
                            callback.onSuccess("Korisnički podaci su inicijalizovani");
                        }
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "Error creating default categories: " + error);
                        if (callback != null) {
                            callback.onError("Greška pri inicijalizaciji: " + error);
                        }
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Error initializing user data", e);
                if (callback != null) {
                    callback.onError("Greška pri inicijalizaciji korisničkih podataka");
                }
            }
        });
    }

    // ========== TASK OPERATIONS ==========

    public void createTask(TaskEntity task, TaskRepository.OnTaskInsertedCallback callback) {
        taskRepository.insertTask(task, callback);
    }

    public void updateTask(TaskEntity task) {
        taskRepository.updateTask(task);
    }

    public void deleteTask(TaskEntity task) {
        taskRepository.deleteTask(task);
    }

    public void completeTask(long taskId, String userId, TaskRepository.OnTaskCompletedCallback callback) {
        taskRepository.completeTask(taskId, userId, callback);
    }

    public LiveData<List<TaskEntity>> getAllTasks(String userId) {
        return taskRepository.getAllTasks(userId);
    }

    public LiveData<List<TaskEntity>> getActiveTasks(String userId) {
        return taskRepository.getTasksByStatus(userId, TaskEntity.STATUS_ACTIVE);
    }

    public LiveData<List<TaskEntity>> getCompletedTasks(String userId) {
        return taskRepository.getTasksByStatus(userId, TaskEntity.STATUS_COMPLETED);
    }

    public LiveData<List<TaskEntity>> getTasksForToday(String userId) {
        long startOfDay = com.example.ma2025.utils.DateUtils.getStartOfDay(System.currentTimeMillis());
        long endOfDay = com.example.ma2025.utils.DateUtils.getEndOfDay(System.currentTimeMillis());
        return taskRepository.getTasksForDateRange(userId, startOfDay, endOfDay);
    }

    // ========== CATEGORY OPERATIONS ==========

    public void createCategory(CategoryEntity category, CategoryRepository.OnCategoryOperationCallback callback) {
        categoryRepository.insertCategory(category, callback);
    }

    public void updateCategory(CategoryEntity category, CategoryRepository.OnCategoryOperationCallback callback) {
        categoryRepository.updateCategory(category, callback);
    }

    public void deleteCategory(CategoryEntity category, CategoryRepository.OnCategoryOperationCallback callback) {
        categoryRepository.deleteCategory(category, callback);
    }

    public LiveData<List<CategoryEntity>> getAllCategories(String userId) {
        return categoryRepository.getAllCategories(userId);
    }

    public LiveData<CategoryEntity> getCategoryById(long categoryId) {
        return categoryRepository.getCategoryById(categoryId);
    }

    public void validateCategoryColor(String userId, String color, long excludeCategoryId,
                                      CategoryRepository.OnColorValidationCallback callback) {
        categoryRepository.validateCategoryColor(userId, color, excludeCategoryId, callback);
    }

    // ========== STATISTICS & PROGRESS ==========

    public LiveData<UserProgressEntity> getUserProgress(String userId) {
        return taskRepository.getUserProgress(userId);
    }

    public LiveData<List<DailyStatsEntity>> getLast7DaysStats(String userId) {
        return taskRepository.getLast7DaysStats(userId);
    }

    public void getUserStatistics(String userId, OnStatisticsCallback callback) {
        executor.execute(() -> {
            try {
                // This would collect various statistics from different tables
                // For now, return basic info
                UserProgressEntity progress = taskRepository.getUserProgress(userId).getValue();

                UserStatistics stats = new UserStatistics();
                if (progress != null) {
                    stats.currentLevel = progress.currentLevel;
                    stats.currentXp = progress.currentXp;
                    stats.totalPp = progress.totalPp;
                    stats.coins = progress.coins;
                    stats.currentStreak = progress.currentStreak;
                    stats.longestStreak = progress.longestStreak;
                }

                if (callback != null) {
                    callback.onStatisticsLoaded(stats);
                }

            } catch (Exception e) {
                Log.e(TAG, "Error loading statistics", e);
                if (callback != null) {
                    callback.onError("Greška pri učitavanju statistika");
                }
            }
        });
    }

    // ========== SYNC OPERATIONS ==========

    /**
     * Sync local SQLite data with Firebase
     * Called periodically or when user explicitly requests sync
     */
    public void syncWithFirebase(String userId, OnSyncCallback callback) {
        executor.execute(() -> {
            try {
                Log.d(TAG, "Starting sync with Firebase for user: " + userId);

                // 1. Upload unsynced local data to Firebase
                uploadUnsyncedData(userId);

                // 2. Download user progress from Firebase to update local cache
                downloadUserProgressFromFirebase(userId);

                // 3. Sync categories if needed
                syncCategoriesWithFirebase(userId);

                if (callback != null) {
                    callback.onSyncCompleted("Sinhronizacija završena uspešno");
                }

            } catch (Exception e) {
                Log.e(TAG, "Error during sync", e);
                if (callback != null) {
                    callback.onSyncFailed("Greška pri sinhronizaciji: " + e.getMessage());
                }
            }
        });
    }

    private void uploadUnsyncedData(String userId) {
        // Upload unsynced tasks, categories, etc. to Firebase
        // This would be implemented with actual Firebase operations
    }

    private void downloadUserProgressFromFirebase(String userId) {
        // Download latest user progress from Firebase User document
        // Update local SQLite with any changes
    }

    private void syncCategoriesWithFirebase(String userId) {
        // Sync categories between local SQLite and Firebase
    }

    // ========== DATA CLEANUP ==========

    /**
     * Clear all local data for a user (logout, account deletion)
     */
    public void clearUserData(String userId, OnClearDataCallback callback) {
        executor.execute(() -> {
            try {
                AppDatabase db = AppDatabase.getDatabase(context);

                // Delete all user-specific data
                db.taskDao().deleteAllUserTasks(userId);
                db.categoryDao().deleteAllUserCategories(userId);
                db.dailyStatsDao().deleteAllUserStats(userId); // You'd need to add this
                db.userProgressDao().deleteUserProgress(userId);

                if (callback != null) {
                    callback.onDataCleared("Korisnički podaci su obrisani");
                }

            } catch (Exception e) {
                Log.e(TAG, "Error clearing user data", e);
                if (callback != null) {
                    callback.onError("Greška pri brisanju podataka");
                }
            }
        });
    }

    // ========== BACKUP & RESTORE ==========

    /**
     * Create backup of user data
     */
    public void createBackup(String userId, OnBackupCallback callback) {
        executor.execute(() -> {
            try {
                // Export user data to JSON format
                // This would include tasks, categories, progress, etc.
                String backupData = exportUserDataToJson(userId);

                if (callback != null) {
                    callback.onBackupCreated(backupData);
                }

            } catch (Exception e) {
                Log.e(TAG, "Error creating backup", e);
                if (callback != null) {
                    callback.onError("Greška pri kreiranju backup-a");
                }
            }
        });
    }

    private String exportUserDataToJson(String userId) {
        // TODO: Implement JSON export of user data
        return "{}"; // Placeholder
    }

    // ========== CALLBACKS ==========

    public interface OnInitializationCallback {
        void onSuccess(String message);
        void onError(String error);
    }

    public interface OnStatisticsCallback {
        void onStatisticsLoaded(UserStatistics statistics);
        void onError(String error);
    }

    public interface OnSyncCallback {
        void onSyncCompleted(String message);
        void onSyncFailed(String error);
    }

    public interface OnClearDataCallback {
        void onDataCleared(String message);
        void onError(String error);
    }

    public interface OnBackupCallback {
        void onBackupCreated(String backupData);
        void onError(String error);
    }

    // ========== DATA CLASSES ==========

    public static class UserStatistics {
        public int currentLevel;
        public int currentXp;
        public int totalPp;
        public int coins;
        public int currentStreak;
        public int longestStreak;
        public int totalTasksCreated;
        public int totalTasksCompleted;
        public int activeDays;

        public UserStatistics() {
            // Initialize with default values
        }
    }

    // ========== UTILITY METHODS ==========

    /**
     * Check if local database exists and is initialized
     */
    public boolean isDatabaseInitialized() {
        return AppDatabase.databaseExists(context);
    }

    /**
     * Get database size for storage management
     */
    public void getDatabaseSize(OnDatabaseSizeCallback callback) {
        executor.execute(() -> {
            try {
                // Calculate database file size
                long sizeInBytes = context.getDatabasePath(AppDatabase.DATABASE_NAME).length();
                double sizeInMB = sizeInBytes / (1024.0 * 1024.0);

                if (callback != null) {
                    callback.onSizeCalculated(sizeInMB);
                }

            } catch (Exception e) {
                if (callback != null) {
                    callback.onSizeCalculated(0.0);
                }
            }
        });
    }

    public interface OnDatabaseSizeCallback {
        void onSizeCalculated(double sizeInMB);
    }
}