package com.example.ma2025.data.repositories;

import android.content.Context;
import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
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
import com.example.ma2025.viewmodels.TaskListViewModel;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

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

    // ========== GRACE PERIOD SYSTEM ==========

    /**
     * Automatski označava zadatke koji su prošli grace period kao neurađene
     */
    public void expireOverdueTasks(String userId) {
        executor.execute(() -> {
            try {
                long now = System.currentTimeMillis();
                long expirationThreshold = now - TaskEntity.GRACE_PERIOD_MILLIS;

                // Pronađi sve aktivne zadatke koji su prošli grace period
                List<TaskEntity> expiredTasks = taskDao.getExpiredActiveTasks(userId, expirationThreshold);

                for (TaskEntity task : expiredTasks) {
                    // Označi kao neurađen
                    task.markFailed();
                    taskDao.updateTask(task);

                    Log.d(TAG, "Task expired: " + task.title + " (due: " + task.dueTime + ")");
                }

                if (!expiredTasks.isEmpty()) {
                    Log.d(TAG, "Expired " + expiredTasks.size() + " overdue tasks");
                }

            } catch (Exception e) {
                Log.e(TAG, "Error expiring overdue tasks", e);
            }
        });
    }

    // ========== TASK OPERATIONS ==========

    public LiveData<TaskEntity> getTaskById(long taskId) {
        return taskDao.getTaskById(taskId);
    }

    public TaskEntity getTaskByIdSync(long taskId) {
        return taskDao.getTaskByIdSync(taskId);
    }

    public LiveData<List<TaskEntity>> getTasksByCategory(String userId, long categoryId) {
        return taskDao.getTasksByCategory(userId, categoryId);
    }

    public List<TaskEntity> getOverdueTasks(String userId) {
        return taskDao.getOverdueTasks(userId, System.currentTimeMillis());
    }

    public LiveData<List<TaskEntity>> getOverdueTasksLiveData(String userId) {
        MutableLiveData<List<TaskEntity>> liveData = new MutableLiveData<>();
        executor.execute(() -> {
            List<TaskEntity> overdueTasks = getOverdueTasks(userId);
            liveData.postValue(overdueTasks);
        });
        return liveData;
    }

    public LiveData<Integer> getOverdueTasksCount(String userId) {
        MutableLiveData<Integer> count = new MutableLiveData<>();
        executor.execute(() -> {
            List<TaskEntity> overdueTasks = getOverdueTasks(userId);
            count.postValue(overdueTasks.size());
        });
        return count;
    }

    public List<TaskEntity> getRepeatingTasks(String userId) {
        return taskDao.getRepeatingTasks(userId);
    }

    public LiveData<List<TaskEntity>> getTasksForDate(String userId, long date) {
        long startOfDay = DateUtils.getStartOfDay(date);
        long endOfDay = startOfDay + 24 * 60 * 60 * 1000 - 1;
        return taskDao.getTasksForDateRange(userId, startOfDay, endOfDay);
    }

    public LiveData<List<TaskEntity>> getCurrentAndFutureTasks(String userId) {
        long now = System.currentTimeMillis();
        return taskDao.getTasksForDateRange(userId, now, Long.MAX_VALUE);
    }

    public void insertTask(TaskEntity task, OnTaskInsertedCallback callback) {
        executor.execute(() -> {
            try {
                long taskId = taskDao.insertTask(task);
                task.id = taskId;

                if (task.isRepeating) {
                    generateRepeatingTaskInstances(task);
                }

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

    private void generateRepeatingTaskInstances(TaskEntity originalTask) {
        if (originalTask.startDate == null || originalTask.endDate == null ||
                originalTask.repeatInterval == null || originalTask.repeatUnit == null) {
            Log.w(TAG, "Cannot generate instances - missing data");
            return;
        }

        try {
            long currentDate = originalTask.startDate;
            long intervalInMillis = calculateIntervalInMillis(originalTask.repeatInterval, originalTask.repeatUnit);

            Log.d(TAG, "Generating instances from " + currentDate + " to " + originalTask.endDate +
                    " with interval " + intervalInMillis);

            int instanceCount = 0;
            while (currentDate <= originalTask.endDate && instanceCount < 1000) {
                TaskEntity instance = createTaskInstance(originalTask, currentDate);
                long instanceId = taskDao.insertTask(instance);
                Log.d(TAG, "Created task instance with ID: " + instanceId + " for date: " + currentDate);

                currentDate += intervalInMillis;
                instanceCount++;
            }

            Log.d(TAG, "Generated " + instanceCount + " task instances");

        } catch (Exception e) {
            Log.e(TAG, "Error generating repeating task instances", e);
        }
    }

    private TaskEntity createTaskInstance(TaskEntity original, long dueTime) {
        TaskEntity instance = new TaskEntity();
        instance.userId = original.userId;
        instance.title = original.title;
        instance.description = original.description;
        instance.categoryId = original.categoryId;
        instance.difficulty = original.difficulty;
        instance.importance = original.importance;
        instance.isRepeating = false;
        instance.dueTime = dueTime;
        instance.status = TaskEntity.STATUS_ACTIVE;
        instance.parentTaskId = original.id;

        return instance;
    }

    public void updateRecurringTaskFutureInstances(TaskEntity masterTask) {
        executor.execute(() -> {
            try {
                taskDao.updateTask(masterTask);

                taskDao.updateFutureInstancesOfRecurringTask(
                        masterTask.id,
                        masterTask.title,
                        masterTask.description,
                        masterTask.difficulty,
                        masterTask.importance
                );

                syncTaskToFirebase(masterTask);
            } catch (Exception e) {
                Log.e("TaskRepository", "Error updating recurring task instances", e);
            }
        });
    }

    private long calculateIntervalInMillis(int interval, String unit) {
        long baseUnit;
        switch (unit.toLowerCase()) {
            case "dan":
            case "day":
                baseUnit = 24 * 60 * 60 * 1000L;
                break;
            case "nedelja":
            case "week":
                baseUnit = 7 * 24 * 60 * 60 * 1000L;
                break;
            default:
                baseUnit = 24 * 60 * 60 * 1000L;
                Log.w(TAG, "Unknown repeat unit: " + unit + ", defaulting to day");
        }

        return interval * baseUnit;
    }

    public void updateTask(TaskEntity task) {
        executor.execute(() -> {
            // NOVO: Proveri da li zadatak može da se ažurira
            if (!task.canBeModified()) {
                Log.w(TAG, "Cannot modify failed task: " + task.title);
                return;
            }

            taskDao.updateTask(task);
            syncTaskToFirebase(task);
        });
    }

    public void deleteTask(TaskEntity task) {
        executor.execute(() -> {
            // NOVO: Proveri da li zadatak može da se obriše
            if (!task.canBeDeleted()) {
                Log.w(TAG, "Cannot delete failed task: " + task.title);
                return;
            }

            taskDao.deleteTask(task);
            if (task.firebaseId != null) {
                deleteTaskFromFirebase(task.firebaseId);
            }
        });
    }

    public void deleteRecurringTask(long taskId) {
        executor.execute(() -> {
            try {
                TaskEntity task = taskDao.getTaskByIdSync(taskId);
                if (task != null && task.isRepeating) {
                    long masterTaskId = (task.parentTaskId != null) ? task.parentTaskId : task.id;

                    taskDao.deleteRecurringTaskAndFutureInstances(masterTaskId, TaskEntity.STATUS_COMPLETED);

                    if (task.firebaseId != null) {
                        deleteTaskFromFirebase(task.firebaseId);
                    }
                }
            } catch (Exception e) {
                Log.e("TaskRepository", "Error deleting recurring task", e);
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

    // ========== TASK COMPLETION WITH GRACE PERIOD ==========

    public void completeTask(long taskId, String userId, OnTaskCompletedCallback callback) {
        executor.execute(() -> {
            try {
                TaskEntity task = taskDao.getTaskByIdSync(taskId);
                if (task == null || task.isCompleted()) {
                    if (callback != null) {
                        callback.onError("Task not found or already completed");
                    }
                    return;
                }

                if (!task.canBeCompleted()) {
                    String errorMessage;
                    if (task.isExpired()) {
                        errorMessage = "Zadatak je prosao grace period od 3 dana i ne može se više završiti";
                    } else if (task.status == TaskEntity.STATUS_FAILED) {
                        errorMessage = "Neurađen zadatak se ne može označiti kao završen";
                    } else if (task.status == TaskEntity.STATUS_CANCELED) {
                        errorMessage = "Otkazan zadatak se ne može označiti kao završen";
                    } else if (task.status == TaskEntity.STATUS_PAUSED) {
                        errorMessage = "Pauziran zadatak se ne može označiti kao završen. Prvo ga aktivirajte.";
                    } else {
                        errorMessage = "Zadatak se ne može završiti";
                    }

                    if (callback != null) {
                        callback.onError(errorMessage);
                    }
                    return;
                }

                UserProgressEntity userProgress = userProgressDao.getUserProgressSync(userId);
                if (userProgress == null) {
                    userProgress = new UserProgressEntity(userId);
                    userProgressDao.insertOrUpdateUserProgress(userProgress);
                }

                int xpEarned = 0;

                if (task.isEligibleForXp()) {
                    xpEarned = task.calculateXpValue(userProgress.currentLevel);

                    if (!canEarnXpForTask(task, userId)) {
                        xpEarned = 0;
                    }
                } else {
                    Log.d(TAG, "Task not eligible for XP: status=" + task.status);
                }

                task.markCompleted();
                taskDao.updateTask(task);

                if (!task.canEarnXp()) {
                    xpEarned = 0;
                    Log.d(TAG, "Task cannot earn XP after completion check");
                }

                TaskCompletionEntity completion = new TaskCompletionEntity(taskId, xpEarned);
                taskCompletionDao.insertTaskCompletion(completion);

                if (xpEarned > 0) {
                    userProgress.addXp(xpEarned);

                    int requiredXp = GameLogicUtils.calculateXpForLevel(userProgress.currentLevel + 1);
                    if (userProgress.currentXp >= requiredXp) {
                        int newLevel = userProgress.currentLevel + 1;
                        int ppGained = GameLogicUtils.calculatePpForLevel(newLevel);
                        userProgress.levelUp(newLevel, ppGained);
                    }

                    userProgressDao.updateUserProgress(userProgress);
                }

                updateDailyStats(userId, task, xpEarned);
                updateStreak(userId);

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

    // ========== TASK STATUS CHANGES WITH GRACE PERIOD ==========

    public void pauseTask(long taskId, String userId, OnTaskStatusChangeCallback callback) {
        executor.execute(() -> {
            try {
                TaskEntity task = taskDao.getTaskByIdSync(taskId);
                if (task == null) {
                    if (callback != null) {
                        callback.onError("Task not found");
                    }
                    return;
                }

                // NOVO: Proveri da li zadatak može da se pauzira
                if (!task.canBePaused()) {
                    String errorMessage;
                    if (!task.canBeModified()) {
                        errorMessage = "Neurađen zadatak se ne može pauzirati";
                    } else if (!task.isRepeating) {
                        errorMessage = "Samo ponavljajući zadaci mogu biti pauzirani";
                    } else if (task.status != TaskEntity.STATUS_ACTIVE) {
                        errorMessage = "Zadatak mora biti aktivan da bi mogao da se pauzira";
                    } else {
                        errorMessage = "Zadatak se ne može pauzirati";
                    }

                    if (callback != null) {
                        callback.onError(errorMessage);
                    }
                    return;
                }

                task.pause();
                taskDao.updateTask(task);

                syncTaskToFirebase(task);

                if (callback != null) {
                    callback.onSuccess("Task paused successfully");
                }

            } catch (Exception e) {
                Log.e(TAG, "Error pausing task", e);
                if (callback != null) {
                    callback.onError(e.getMessage());
                }
            }
        });
    }

    public void resumeTask(long taskId, String userId, OnTaskStatusChangeCallback callback) {
        executor.execute(() -> {
            try {
                TaskEntity task = taskDao.getTaskByIdSync(taskId);
                if (task == null) {
                    if (callback != null) {
                        callback.onError("Task not found");
                    }
                    return;
                }

                // NOVO: Proveri da li zadatak može da se nastavi
                if (!task.canBeResumed()) {
                    String errorMessage;
                    if (!task.canBeModified()) {
                        errorMessage = "Neurađen zadatak se ne može nastaviti";
                    } else if (task.status != TaskEntity.STATUS_PAUSED) {
                        errorMessage = "Zadatak mora biti pauziran da bi mogao da se nastavi";
                    } else {
                        errorMessage = "Zadatak se ne može nastaviti";
                    }

                    if (callback != null) {
                        callback.onError(errorMessage);
                    }
                    return;
                }

                task.activate();
                taskDao.updateTask(task);

                syncTaskToFirebase(task);

                if (callback != null) {
                    callback.onSuccess("Task resumed successfully");
                }

            } catch (Exception e) {
                Log.e(TAG, "Error resuming task", e);
                if (callback != null) {
                    callback.onError(e.getMessage());
                }
            }
        });
    }

    public void cancelTask(long taskId, String userId, OnTaskStatusChangeCallback callback) {
        executor.execute(() -> {
            try {
                TaskEntity task = taskDao.getTaskByIdSync(taskId);
                if (task == null) {
                    if (callback != null) {
                        callback.onError("Task not found");
                    }
                    return;
                }

                if (!task.canBeModified()) {
                    String errorMessage;
                    if (task.status == TaskEntity.STATUS_FAILED) {
                        errorMessage = "Neurađen zadatak se ne može otkazati";
                    } else if (task.status == TaskEntity.STATUS_CANCELED) {
                        errorMessage = "Zadatak je već otkazan";
                    } else {
                        errorMessage = "Zadatak se ne može otkazati";
                    }

                    if (callback != null) {
                        callback.onError(errorMessage);
                    }
                    return;
                }

                task.markCanceled();
                taskDao.updateTask(task);

                syncTaskToFirebase(task);

                if (callback != null) {
                    callback.onSuccess("Task cancelled successfully");
                }

            } catch (Exception e) {
                Log.e(TAG, "Error cancelling task", e);
                if (callback != null) {
                    callback.onError(e.getMessage());
                }
            }
        });
    }

    public void failTask(long taskId, String userId, OnTaskCompletedCallback callback) {
        executor.execute(() -> {
            try {
                TaskEntity task = taskDao.getTaskByIdSync(taskId);
                if (task == null || task.status != TaskEntity.STATUS_ACTIVE) {
                    if (callback != null) {
                        callback.onError("Task not found or not active");
                    }
                    return;
                }

                task.markFailed();
                taskDao.updateTask(task);

                updateDailyStatsForFailedTask(userId, task);

                if (callback != null) {
                    callback.onSuccess(0, 0);
                }

            } catch (Exception e) {
                Log.e(TAG, "Error failing task", e);
                if (callback != null) {
                    callback.onError(e.getMessage());
                }
            }
        });
    }

    // ========== BATCH OPERATIONS ==========

    public void completeBatchTasks(List<Long> taskIds, String userId, OnBatchOperationCallback callback) {
        executor.execute(() -> {
            try {
                int completedCount = 0;
                int totalXpEarned = 0;

                UserProgressEntity userProgress = userProgressDao.getUserProgressSync(userId);
                if (userProgress == null) {
                    userProgress = new UserProgressEntity(userId);
                    userProgressDao.insertOrUpdateUserProgress(userProgress);
                }

                for (Long taskId : taskIds) {
                    TaskEntity task = taskDao.getTaskByIdSync(taskId);
                    if (task != null && task.canBeCompleted()) {

                        int xpEarned = task.calculateXpValue(userProgress.currentLevel);

                        if (!canEarnXpForTask(task, userId)) {
                            xpEarned = 0;
                        }

                        task.markCompleted();
                        taskDao.updateTask(task);

                        TaskCompletionEntity completion = new TaskCompletionEntity(taskId, xpEarned);
                        taskCompletionDao.insertTaskCompletion(completion);

                        updateDailyStats(userId, task, xpEarned);

                        totalXpEarned += xpEarned;
                        completedCount++;

                        syncTaskToFirebase(task);
                    }
                }

                if (totalXpEarned > 0) {
                    userProgress.addXp(totalXpEarned);

                    int requiredXp = GameLogicUtils.calculateXpForLevel(userProgress.currentLevel + 1);
                    if (userProgress.currentXp >= requiredXp) {
                        int newLevel = userProgress.currentLevel + 1;
                        int ppGained = GameLogicUtils.calculatePpForLevel(newLevel);
                        userProgress.levelUp(newLevel, ppGained);
                    }

                    userProgressDao.updateUserProgress(userProgress);
                    syncUserProgressToFirebase(userProgress);
                }

                updateStreak(userId);

                if (callback != null) {
                    callback.onSuccess(completedCount, totalXpEarned);
                }

            } catch (Exception e) {
                Log.e(TAG, "Error in batch task completion", e);
                if (callback != null) {
                    callback.onError(e.getMessage());
                }
            }
        });
    }

    public void deleteBatchTasks(List<TaskEntity> tasks) {
        executor.execute(() -> {
            try {
                for (TaskEntity task : tasks) {
                    if (task.canBeDeleted()) {
                        taskDao.deleteTask(task);

                        if (task.firebaseId != null) {
                            deleteTaskFromFirebase(task.firebaseId);
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in batch task deletion", e);
            }
        });
    }

    // ========== XP QUOTA SYSTEM ==========

    private boolean canEarnXpForTask(TaskEntity task, String userId) {
        if (!checkDifficultyQuota(task.difficulty, userId)) {
            return false;
        }

        if (!checkImportanceQuota(task.importance, userId)) {
            return false;
        }

        return true;
    }

    private boolean checkDifficultyQuota(int difficulty, String userId) {
        switch (difficulty) {
            case TaskEntity.DIFFICULTY_VERY_EASY:
                return getDailyCompletedCount(userId, difficulty) < 5;

            case TaskEntity.DIFFICULTY_EASY:
                return getDailyCompletedCount(userId, difficulty) < 5;

            case TaskEntity.DIFFICULTY_HARD:
                return getDailyCompletedCount(userId, difficulty) < 2;

            case TaskEntity.DIFFICULTY_EXTREME:
                return getWeeklyCompletedCount(userId, difficulty) < 1;

            default:
                return true;
        }
    }

    private boolean checkImportanceQuota(int importance, String userId) {
        switch (importance) {
            case TaskEntity.IMPORTANCE_NORMAL:
                return getDailyCompletedCountByImportance(userId, importance) < 5;

            case TaskEntity.IMPORTANCE_IMPORTANT:
                return getDailyCompletedCountByImportance(userId, importance) < 5;

            case TaskEntity.IMPORTANCE_VERY_IMPORTANT:
                return getDailyCompletedCountByImportance(userId, importance) < 2;

            case TaskEntity.IMPORTANCE_SPECIAL:
                return getMonthlyCompletedCountByImportance(userId, importance) < 1;

            default:
                return true;
        }
    }

    private int getDailyCompletedCount(String userId, int difficulty) {
        long today = DateUtils.getStartOfDay(System.currentTimeMillis());
        long tomorrow = today + 24 * 60 * 60 * 1000;
        return taskDao.getCompletedTasksCountByDifficultyAndDateRange(userId, difficulty, today, tomorrow);
    }

    private int getDailyCompletedCountByImportance(String userId, int importance) {
        long today = DateUtils.getStartOfDay(System.currentTimeMillis());
        long tomorrow = today + 24 * 60 * 60 * 1000;
        return taskDao.getCompletedTasksCountByImportanceAndDateRange(userId, importance, today, tomorrow);
    }

    private int getWeeklyCompletedCount(String userId, int difficulty) {
        long startOfWeek = DateUtils.getStartOfWeek(System.currentTimeMillis());
        long endOfWeek = startOfWeek + 7 * 24 * 60 * 60 * 1000;
        return taskDao.getCompletedTasksCountByDifficultyAndDateRange(userId, difficulty, startOfWeek, endOfWeek);
    }

    private int getMonthlyCompletedCountByImportance(String userId, int importance) {
        long startOfMonth = DateUtils.getStartOfMonth(System.currentTimeMillis());
        long endOfMonth = DateUtils.getEndOfMonth(System.currentTimeMillis());
        return taskDao.getCompletedTasksCountByImportanceAndDateRange(userId, importance, startOfMonth, endOfMonth);
    }

    // ========== STATISTICS ==========

    public LiveData<TaskListViewModel.TaskStatistics> getTaskStatistics(String userId) {
        MutableLiveData<TaskListViewModel.TaskStatistics> statisticsLiveData = new MutableLiveData<>();

        executor.execute(() -> {
            try {
                int totalTasks = taskDao.getTotalTasks(userId);
                int completedTasks = taskDao.getTotalCompletedTasks(userId);

                int activeTasks = 0;
                int failedTasks = 0;
                int pausedTasks = 0;
                int canceledTasks = 0;

                List<TaskEntity> allTasks = taskDao.getAllTasks(userId).getValue();
                if (allTasks != null) {
                    for (TaskEntity task : allTasks) {
                        switch (task.status) {
                            case TaskEntity.STATUS_ACTIVE:
                                activeTasks++;
                                break;
                            case TaskEntity.STATUS_FAILED:
                                failedTasks++;
                                break;
                            case TaskEntity.STATUS_PAUSED:
                                pausedTasks++;
                                break;
                            case TaskEntity.STATUS_CANCELED:
                                canceledTasks++;
                                break;
                        }
                    }
                }

                int overdueCount = getOverdueTasks(userId).size();

                TaskListViewModel.TaskStatistics statistics = new TaskListViewModel.TaskStatistics(
                        totalTasks, completedTasks, activeTasks, failedTasks, pausedTasks, overdueCount
                );

                statisticsLiveData.postValue(statistics);

            } catch (Exception e) {
                Log.e(TAG, "Error calculating task statistics", e);
                statisticsLiveData.postValue(new TaskListViewModel.TaskStatistics(0, 0, 0, 0, 0, 0));
            }
        });

        return statisticsLiveData;
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

        while (true) {
            int completions = taskDao.getTasksCompletedOnDate(userId, checkDate);
            if (completions > 0) {
                streak++;
                checkDate -= 24 * 60 * 60 * 1000;
            } else {
                break;
            }

            if (streak > 365) break;
        }

        return streak;
    }

    private void updateDailyStatsForFailedTask(String userId, TaskEntity task) {
        long today = DateUtils.getStartOfDay(System.currentTimeMillis());
        DailyStatsEntity stats = dailyStatsDao.getDailyStats(userId, today);

        if (stats == null) {
            stats = new DailyStatsEntity(userId, today);
        }

        stats.incrementTaskFailed();
        dailyStatsDao.insertOrUpdateDailyStats(stats);
    }

    // ========== FIREBASE SYNC ==========

    private void syncTaskToFirebase(TaskEntity task) {
        if (task.syncedToFirebase) return;

        executor.execute(() -> {
            try {
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

    // ========== STATISTICS (EXISTING) ==========

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

    // ========== CALLBACK INTERFACES ==========

    public interface OnTaskInsertedCallback {
        void onSuccess(long taskId);
        void onError(String error);
    }

    public interface OnTaskCompletedCallback {
        void onSuccess(int xpEarned, int newLevel);
        void onError(String error);
    }

    public interface OnTaskStatusChangeCallback {
        void onSuccess(String message);
        void onError(String error);
    }

    public interface OnBatchOperationCallback {
        void onSuccess(int count, int totalXp);
        void onError(String error);
    }
}