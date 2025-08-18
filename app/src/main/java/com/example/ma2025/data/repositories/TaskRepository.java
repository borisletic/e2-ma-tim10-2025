// Fixed TaskRepository.java - Corrected query usage
package com.example.ma2025.data.repositories;

import android.content.Context;
import android.util.Log;
import androidx.lifecycle.LiveData;
import com.example.ma2025.data.database.AppDatabase;
import com.example.ma2025.data.database.entities.TaskEntity;
import com.example.ma2025.data.database.entities.TaskCompletionEntity;
import com.example.ma2025.data.database.entities.DailyStatsEntity;
import com.example.ma2025.data.database.entities.UserProgressEntity;
import com.example.ma2025.data.database.dao.TaskDao;
import com.example.ma2025.data.database.dao.TaskCompletionDao;
import com.example.ma2025.data.database.dao.DailyStatsDao;
import com.example.ma2025.data.database.dao.UserProgressDao;
import com.example.ma2025.utils.DateUtils;
import com.example.ma2025.utils.GameLogicUtils;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TaskRepository {
    private static final String TAG = "TaskRepository";

    private TaskDao taskDao;
    private TaskCompletionDao taskCompletionDao;
    private DailyStatsDao dailyStatsDao;
    private UserProgressDao userProgressDao;
    private FirebaseFirestore firestore;
    private ExecutorService executor;

    private static volatile TaskRepository INSTANCE;

    private TaskRepository(Context context) {
        AppDatabase database = AppDatabase.getDatabase(context);
        taskDao = database.taskDao();
        taskCompletionDao = database.taskCompletionDao();
        dailyStatsDao = database.dailyStatsDao();
        userProgressDao = database.userProgressDao();
        firestore = FirebaseFirestore.getInstance();
        executor = Executors.newFixedThreadPool(4);
    }

    public static TaskRepository getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (TaskRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new TaskRepository(context);
                }
            }
        }
        return INSTANCE;
    }

    // ========== TASK OPERATIONS ==========

    public void insertTask(TaskEntity task, OnTaskInsertedCallback callback) {
        executor.execute(() -> {
            try {
                long taskId = taskDao.insertTask(task);
                task.id = taskId;

                // Sync to Firebase in background
                syncTaskToFirebase(task);

                if (callback != null) {
                    callback.onSuccess(taskId);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error inserting task", e);
                if (callback != null) {
                    callback.onError(e.getMessage());
                }
            }
        });
    }

    public void updateTask(TaskEntity task) {
        executor.execute(() -> {
            taskDao.updateTask(task);
            syncTaskToFirebase(task);
        });
    }

    public void deleteTask(TaskEntity task) {
        executor.execute(() -> {
            taskDao.deleteTask(task);
            // Also delete from Firebase if synced
            if (task.firebaseId != null) {
                deleteTaskFromFirebase(task.firebaseId);
            }
        });
    }

    public LiveData<List<TaskEntity>> getAllTasks(String userId) {
        return taskDao.getAllTasks(userId);
    }

    public LiveData<List<TaskEntity>> getTasksByStatus(String userId, int status) {
        return taskDao.getTasksByStatus(userId, status);
    }

    public LiveData<List<TaskEntity>> getTasksForDateRange(String userId, long startTime, long endTime) {
        return taskDao.getTasksForDateRange(userId, startTime, endTime);
    }

    // ========== TASK COMPLETION ==========

    public void completeTask(long taskId, String userId, OnTaskCompletedCallback callback) {
        executor.execute(() -> {
            try {
                // FIXED: Use synchronous method to get task
                TaskEntity task = taskDao.getTaskByIdSync(taskId);
                if (task == null || task.isCompleted()) {
                    if (callback != null) {
                        callback.onError("Task not found or already completed");
                    }
                    return;
                }

                // Get user progress for XP calculation
                UserProgressEntity userProgress = userProgressDao.getUserProgressSync(userId);
                if (userProgress == null) {
                    userProgress = new UserProgressEntity(userId);
                    userProgressDao.insertOrUpdateUserProgress(userProgress);
                }

                // Calculate XP
                int xpEarned = task.calculateXpValue(userProgress.currentLevel);

                // Check daily quotas
                if (!canEarnXpForTask(task, userId)) {
                    xpEarned = 0; // Exceeds quota, no XP
                }

                // Mark task as completed
                task.markCompleted();
                taskDao.updateTask(task);

                // Record completion
                TaskCompletionEntity completion = new TaskCompletionEntity(taskId, xpEarned);
                taskCompletionDao.insertTaskCompletion(completion);

                // Update user progress
                if (xpEarned > 0) {
                    userProgress.addXp(xpEarned);

                    // Check for level up
                    int requiredXp = GameLogicUtils.calculateXpForLevel(userProgress.currentLevel + 1);
                    if (userProgress.currentXp >= requiredXp) {
                        int newLevel = userProgress.currentLevel + 1;
                        int ppGained = GameLogicUtils.calculatePpForLevel(newLevel);
                        userProgress.levelUp(newLevel, ppGained);
                    }

                    userProgressDao.updateUserProgress(userProgress);
                }

                // Update daily stats
                updateDailyStats(userId, task, xpEarned);

                // Update streak
                updateStreak(userId);

                // Sync to Firebase
                syncTaskToFirebase(task);
                syncUserProgressToFirebase(userProgress);

                if (callback != null) {
                    callback.onSuccess(xpEarned, userProgress.currentLevel);
                }

            } catch (Exception e) {
                Log.e(TAG, "Error completing task", e);
                if (callback != null) {
                    callback.onError(e.getMessage());
                }
            }
        });
    }

    private boolean canEarnXpForTask(TaskEntity task, String userId) {
        long today = DateUtils.getStartOfDay(System.currentTimeMillis());

        // Check difficulty quota - FIXED: Now uses correct query with JOIN
        int difficultyCount = taskDao.getCompletedTasksCountByDifficultyAndDate(userId, task.difficulty, today);
        int difficultyLimit = getDifficultyDailyLimit(task.difficulty);

        // Check importance quota - FIXED: Now uses correct query with JOIN
        int importanceCount = taskDao.getCompletedTasksCountByImportanceAndDate(userId, task.importance, today);
        int importanceLimit = getImportanceDailyLimit(task.importance);

        return difficultyCount < difficultyLimit && importanceCount < importanceLimit;
    }

    private int getDifficultyDailyLimit(int difficulty) {
        switch (difficulty) {
            case TaskEntity.DIFFICULTY_VERY_EASY: return 5; // daily
            case TaskEntity.DIFFICULTY_EASY: return 5; // daily
            case TaskEntity.DIFFICULTY_HARD: return 2; // daily
            case TaskEntity.DIFFICULTY_EXTREME: return 1; // weekly (converted to daily = 1/7)
            default: return 5;
        }
    }

    private int getImportanceDailyLimit(int importance) {
        switch (importance) {
            case TaskEntity.IMPORTANCE_NORMAL: return 5; // daily
            case TaskEntity.IMPORTANCE_IMPORTANT: return 5; // daily
            case TaskEntity.IMPORTANCE_VERY_IMPORTANT: return 2; // daily
            case TaskEntity.IMPORTANCE_SPECIAL: return 1; // monthly (converted to daily = 1/30)
            default: return 5;
        }
    }

    private void updateDailyStats(String userId, TaskEntity task, int xpEarned) {
        long today = DateUtils.getStartOfDay(System.currentTimeMillis());
        DailyStatsEntity stats = dailyStatsDao.getDailyStats(userId, today);

        if (stats == null) {
            stats = new DailyStatsEntity(userId, today);
        }

        stats.incrementTaskCompleted(task.difficulty);
        stats.addXp(xpEarned);

        dailyStatsDao.insertOrUpdateDailyStats(stats);
    }

    private void updateStreak(String userId) {
        // Calculate current streak based on daily completions
        long today = DateUtils.getStartOfDay(System.currentTimeMillis());
        int currentStreak = calculateCurrentStreak(userId, today);

        UserProgressEntity userProgress = userProgressDao.getUserProgressSync(userId);
        if (userProgress != null) {
            userProgress.updateStreak(currentStreak);
            userProgressDao.updateUserProgress(userProgress);
        }
    }

    private int calculateCurrentStreak(String userId, long today) {
        int streak = 0;
        long checkDate = today;

        // Go backwards day by day until we find a day with no completions
        while (true) {
            int completions = taskDao.getTasksCompletedOnDate(userId, checkDate);
            if (completions > 0) {
                streak++;
                checkDate -= 24 * 60 * 60 * 1000; // Go back one day
            } else {
                break;
            }

            // Prevent infinite loop
            if (streak > 365) break;
        }

        return streak;
    }

    // ========== FIREBASE SYNC ==========

    private void syncTaskToFirebase(TaskEntity task) {
        if (task.syncedToFirebase) return;

        executor.execute(() -> {
            try {
                // TODO: Implement full Firebase sync
                task.syncedToFirebase = true;
                taskDao.updateTask(task);
            } catch (Exception e) {
                Log.e(TAG, "Error syncing task to Firebase", e);
            }
        });
    }

    private void syncUserProgressToFirebase(UserProgressEntity userProgress) {
        // TODO: Sync user progress to Firebase User document
    }

    private void deleteTaskFromFirebase(String firebaseId) {
        // TODO: Delete task from Firebase
    }

    // ========== CALLBACKS ==========

    public interface OnTaskInsertedCallback {
        void onSuccess(long taskId);
        void onError(String error);
    }

    public interface OnTaskCompletedCallback {
        void onSuccess(int xpEarned, int newLevel);
        void onError(String error);
    }

    // ========== STATISTICS ==========

    public LiveData<List<DailyStatsEntity>> getLast7DaysStats(String userId) {
        return dailyStatsDao.getLast7DaysStats(userId);
    }

    public LiveData<UserProgressEntity> getUserProgress(String userId) {
        return userProgressDao.getUserProgress(userId);
    }

    public void initializeUserProgress(String userId) {
        executor.execute(() -> {
            UserProgressEntity existing = userProgressDao.getUserProgressSync(userId);
            if (existing == null) {
                UserProgressEntity newProgress = new UserProgressEntity(userId);
                userProgressDao.insertOrUpdateUserProgress(newProgress);
            }
        });
    }
}