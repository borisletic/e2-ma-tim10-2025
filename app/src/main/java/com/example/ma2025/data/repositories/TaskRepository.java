package com.example.ma2025.data.repositories;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
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
import com.example.ma2025.data.models.Alliance;
import com.example.ma2025.utils.Constants;
import com.example.ma2025.utils.DateUtils;
import com.example.ma2025.utils.GameLogicUtils;
import com.example.ma2025.viewmodels.CreateTaskViewModel;
import com.example.ma2025.viewmodels.TaskListViewModel;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    public boolean doesTaskExceedQuota(TaskEntity task) {
        return !canEarnXpForTask(task, task.userId);
    }

    public void getTasksForPeriod(String userId, long startTime, long endTime, OnTasksRetrievedCallback callback) {
        executor.execute(() -> {
            try {
                List<TaskEntity> tasks = taskDao.getTasksCreatedInPeriod(userId, startTime, endTime);
                if (callback != null) {
                    new Handler(Looper.getMainLooper()).post(() ->
                            callback.onTasksRetrieved(tasks));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting tasks for period", e);
                if (callback != null) {
                    new Handler(Looper.getMainLooper()).post(() ->
                            callback.onTasksRetrieved(new ArrayList<>()));
                }
            }
        });
    }

    public interface OnTasksRetrievedCallback {
        void onTasksRetrieved(List<TaskEntity> tasks);
    }

    public void insertTask(TaskEntity task, OnTaskInsertedCallback callback) {
        executor.execute(() -> {
            try {
                long taskId = taskDao.insertTask(task);
                task.id = taskId;

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

                // Proveri samo grace period i status - UKLONI kvotu odavde
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

                // Izračunaj XP - kvota utiče SAMO na XP, ne na završavanje
                int xpEarned = 0;
                if (task.isEligibleForXp()) {
                    xpEarned = task.calculateXpValue(userProgress.currentLevel);

                    // Kvota sprečava samo XP, ne završavanje zadatka
                    if (!canEarnXpForTask(task, userId)) {
                        xpEarned = 0;
                    }
                }

                // UVEK završi zadatak (osim ponavljajuće)
                if (!task.isRepeating) {
                    task.markCompleted();
                    taskDao.updateTask(task);
                }

                // Zapis o završetku zadatka
                TaskCompletionEntity completion = new TaskCompletionEntity(taskId, xpEarned);
                completion.completionDate = DateUtils.getStartOfDay(System.currentTimeMillis());
                taskCompletionDao.insertTaskCompletion(completion);

                // Ažuriraj korisnikov progres samo ako je dobio XP
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

                checkAndUpdateSpecialMission(userId, task);

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

                // Ažuriraj specijalnu misiju za neuspešan zadatak
                checkAndUpdateSpecialMissionForFailedTask(userId);

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
                int veryEasyCount = getDailyCompletedCount(userId, difficulty);
                return veryEasyCount < 5;

            case TaskEntity.DIFFICULTY_EASY:
                int easyCount = getDailyCompletedCount(userId, difficulty);
                return easyCount < 5;

            case TaskEntity.DIFFICULTY_HARD:
                int hardCount = getDailyCompletedCount(userId, difficulty);
                return hardCount < 2;

            case TaskEntity.DIFFICULTY_EXTREME:
                int extremeCount = getWeeklyCompletedCount(userId, difficulty);
                return extremeCount < 1;

            default:
                return true;
        }
    }

    private boolean checkImportanceQuota(int importance, String userId) {
        switch (importance) {
            case TaskEntity.IMPORTANCE_NORMAL:
                int normalCount = getDailyCompletedCountByImportance(userId, importance);
                return normalCount < 5;

            case TaskEntity.IMPORTANCE_IMPORTANT:
                int importantCount = getDailyCompletedCountByImportance(userId, importance);
                return importantCount < 5;

            case TaskEntity.IMPORTANCE_VERY_IMPORTANT:
                int veryImportantCount = getDailyCompletedCountByImportance(userId, importance);
                return veryImportantCount < 2;

            case TaskEntity.IMPORTANCE_SPECIAL:
                int specialCount = getMonthlyCompletedCountByImportance(userId, importance);
                return specialCount < 1;

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

    // Dodajte ove metode u TaskRepository.java
    public int getCompletedTasksCountByDifficultyForDate(String userId, int difficulty, long date) {
        long dayStart = DateUtils.getStartOfDay(date);
        long dayEnd = dayStart + 24 * 60 * 60 * 1000;
        return taskDao.getCompletedTasksCountByDifficultyAndDateRange(userId, difficulty, dayStart, dayEnd);
    }

    public int getCompletedTasksCountByImportanceForDate(String userId, int importance, long date) {
        long dayStart = DateUtils.getStartOfDay(date);
        long dayEnd = dayStart + 24 * 60 * 60 * 1000;
        return taskDao.getCompletedTasksCountByImportanceAndDateRange(userId, importance, dayStart, dayEnd);
    }

    public int getWeeklyCompletedTasksCountForDate(String userId, int difficulty, long date) {
        long weekStart = DateUtils.getStartOfWeek(date);
        long weekEnd = weekStart + 7 * 24 * 60 * 60 * 1000;
        return taskDao.getCompletedTasksCountByDifficultyAndDateRange(userId, difficulty, weekStart, weekEnd);
    }

    public int getMonthlyCompletedTasksCountByImportanceForDate(String userId, int importance, long date) {
        long monthStart = DateUtils.getStartOfMonth(date);
        long monthEnd = DateUtils.getEndOfMonth(date);
        return taskDao.getCompletedTasksCountByImportanceAndDateRange(userId, importance, monthStart, monthEnd);
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

    public void updateUserProgress(UserProgressEntity userProgress) {
        executor.execute(() -> {
            try {
                userProgressDao.updateUserProgress(userProgress);

                // Sync to Firebase ako treba
                syncUserProgressToFirebase(userProgress);

                Log.d(TAG, "UserProgress updated: Level " + userProgress.currentLevel +
                        ", XP " + userProgress.currentXp);

            } catch (Exception e) {
                Log.e(TAG, "Error updating user progress", e);
            }
        });
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
        try {
            Map<String, Object> progressData = new HashMap<>();
            progressData.put("level", userProgress.currentLevel);
            progressData.put("xp", userProgress.currentXp);
            progressData.put("pp", userProgress.totalPp);
            progressData.put("coins", userProgress.coins);
            progressData.put("currentStreak", userProgress.currentStreak);
            progressData.put("longestStreak", userProgress.longestStreak);
            progressData.put("title", getTitleForLevel(userProgress.currentLevel));
            progressData.put("updatedAt", System.currentTimeMillis());

            firestore.collection(Constants.COLLECTION_USERS)
                    .document(userProgress.userId)
                    .update(progressData)
                    .addOnSuccessListener(aVoid ->
                            Log.d(TAG, "UserProgress synced to Firebase"))
                    .addOnFailureListener(e ->
                            Log.e(TAG, "Error syncing UserProgress to Firebase", e));

        } catch (Exception e) {
            Log.e(TAG, "Error in syncUserProgressToFirebase", e);
        }
    }

    private String getTitleForLevel(int level) {
        switch (level) {
            case 0: return "Novajlija";
            case 1: return "Početnik";
            case 2: return "Istraživač";
            case 3: return "Ratnik";
            case 4: return "Veteran";
            case 5: return "Majstor";
            case 6: return "Ekspert";
            case 7: return "Šampion";
            case 8: return "Legenda";
            case 9: return "Mitska Legenda";
            case 10: return "Besmrtni";
            default: return "Legenda (Nivo " + level + ")";
        }
    }

    public void resetUserProgress(String userId, OnTaskStatusChangeCallback callback) {
        executor.execute(() -> {
            try {
                UserProgressEntity userProgress = new UserProgressEntity(userId);
                userProgressDao.insertOrUpdateUserProgress(userProgress);

                syncUserProgressToFirebase(userProgress);

                if (callback != null) {
                    callback.onSuccess("Napredak resetovan");
                }

                Log.d(TAG, "User progress reset for: " + userId);

            } catch (Exception e) {
                Log.e(TAG, "Error resetting user progress", e);
                if (callback != null) {
                    callback.onError("Greška pri resetovanju napretka");
                }
            }
        });
    }

    public void getCurrentUserLevel(String userId, CreateTaskViewModel.OnUserLevelCallback callback) {
        executor.execute(() -> {
            try {
                UserProgressEntity userProgress = getUserProgressSync(userId);

                if (callback != null) {
                    callback.onUserLevel(userProgress.currentLevel);
                }

            } catch (Exception e) {
                Log.e(TAG, "Error getting current user level", e);
                if (callback != null) {
                    callback.onUserLevel(0);
                }
            }
        });
    }

    public interface OnUserLevelCallback {
        void onUserLevel(int level);
    }

    private void deleteTaskFromFirebase(String firebaseId) {
        // TODO: Delete task from Firebase
    }

    // ========== STATISTICS (EXISTING) ==========

    public LiveData<List<DailyStatsEntity>> getLast7DaysStats(String userId) {
        return dailyStatsDao.getLast7DaysStats(userId);
    }

    public LiveData<UserProgressEntity> getUserProgress(String userId) {
        // Ensure UserProgress exists
        executor.execute(() -> {
            try {
                UserProgressEntity existing = userProgressDao.getUserProgressSync(userId);
                if (existing == null) {
                    Log.d(TAG, "Creating new UserProgress for user: " + userId);
                    UserProgressEntity newProgress = new UserProgressEntity(userId);
                    userProgressDao.insertOrUpdateUserProgress(newProgress);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error ensuring user progress exists", e);
            }
        });

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

    public UserProgressEntity getUserProgressSync(String userId) {
        try {
            UserProgressEntity progress = userProgressDao.getUserProgressSync(userId);
            if (progress == null) {
                progress = new UserProgressEntity(userId);
                userProgressDao.insertOrUpdateUserProgress(progress);
            }
            return progress;
        } catch (Exception e) {
            Log.e(TAG, "Error getting user progress sync", e);
            return new UserProgressEntity(userId);
        }
    }

    private boolean shouldLevelUp(UserProgressEntity userProgress) {
        try {
            int requiredXp = GameLogicUtils.calculateXpForLevel(userProgress.currentLevel + 1);
            return userProgress.currentXp >= requiredXp;
        } catch (Exception e) {
            Log.e(TAG, "Error checking level up", e);
            return false;
        }
    }

    public void addXpToUser(String userId, int xpToAdd, OnTaskCompletedCallback callback) {
        executor.execute(() -> {
            try {
                UserProgressEntity userProgress = getUserProgressSync(userId);

                int oldLevel = userProgress.currentLevel;
                int oldXp = userProgress.currentXp;

                // Add XP
                userProgress.addXp(xpToAdd);

                // Check for level up
                boolean leveledUp = false;
                while (shouldLevelUp(userProgress)) {
                    int newLevel = userProgress.currentLevel + 1;
                    int ppGained = GameLogicUtils.calculatePpForLevel(newLevel);
                    userProgress.levelUp(newLevel, ppGained);
                    leveledUp = true;

                    Log.d(TAG, String.format("Level up! %d -> %d, PP gained: %d",
                            oldLevel, newLevel, ppGained));
                }

                // Save to database
                userProgressDao.updateUserProgress(userProgress);

                // Sync to Firebase
                syncUserProgressToFirebase(userProgress);

                if (callback != null) {
                    callback.onSuccess(xpToAdd, userProgress.currentLevel);
                }

                Log.d(TAG, String.format("XP added: %d -> %d (+%d), Level: %d",
                        oldXp, userProgress.currentXp, xpToAdd, userProgress.currentLevel));

            } catch (Exception e) {
                Log.e(TAG, "Error adding XP to user", e);
                if (callback != null) {
                    callback.onError("Greška pri dodavanju XP");
                }
            }
        });
    }

    private void checkAndUpdateSpecialMission(String userId, TaskEntity task) {
        AllianceRepository allianceRepo = new AllianceRepository();
        allianceRepo.getUserAlliance(userId, new AllianceRepository.OnAllianceLoadedListener() {
            @Override
            public void onSuccess(Alliance alliance) {
                SpecialMissionRepository.getInstance().getActiveMission(alliance.getId())
                        .observeForever(mission -> {
                            if (mission != null) {
                                String actionType;

                                // Određivanje tipa akcije
                                if (task.difficulty == TaskEntity.DIFFICULTY_VERY_EASY ||
                                        task.difficulty == TaskEntity.DIFFICULTY_EASY) {
                                    actionType = "easy_task";
                                } else {
                                    actionType = "hard_task";
                                }

                                // JEDAN poziv za update - MissionProgress će sam da obradi štetu
                                SpecialMissionRepository.getInstance().updateMissionProgress(
                                        mission.getId(), userId, actionType,
                                        new SpecialMissionRepository.OnProgressUpdatedCallback() {
                                            @Override
                                            public void onSuccess(int damageDealt, int remainingBossHp) {
                                                Log.d(TAG, "Special mission progress updated: " + damageDealt + " damage");
                                            }

                                            @Override
                                            public void onError(String error) {
                                                Log.e(TAG, "Special mission update failed: " + error);
                                            }
                                        }
                                );
                            }
                        });
            }

            @Override
            public void onError(String error) {
                Log.d(TAG, "No alliance found for user: " + error);
            }

            @Override
            public void onNotInAlliance() {
                Log.d(TAG, "User not in alliance, skipping special mission update");
            }
        });
    }

    private void checkAndUpdateSpecialMissionForFailedTask(String userId) {
        AllianceRepository allianceRepo = new AllianceRepository();
        allianceRepo.getUserAlliance(userId, new AllianceRepository.OnAllianceLoadedListener() {
            @Override
            public void onSuccess(Alliance alliance) {
                SpecialMissionRepository.getInstance().getActiveMission(alliance.getId())
                        .observeForever(mission -> {
                            if (mission != null) {
                                SpecialMissionRepository.getInstance().updateMissionProgress(
                                        mission.getId(), userId, "task_failed",
                                        new SpecialMissionRepository.OnProgressUpdatedCallback() {
                                            @Override
                                            public void onSuccess(int damageDealt, int remainingBossHp) {
                                                Log.d(TAG, "Special mission updated for failed task (no damage)");
                                            }

                                            @Override
                                            public void onError(String error) {
                                                Log.e(TAG, "Special mission update failed for failed task: " + error);
                                            }
                                        }
                                );
                            }
                        });
            }

            @Override
            public void onError(String error) {
                Log.d(TAG, "No alliance found for failed task update: " + error);
            }

            @Override
            public void onNotInAlliance() {
                Log.d(TAG, "User not in alliance, skipping failed task update");
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