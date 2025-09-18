// Fixed TaskDao.java
package com.example.ma2025.data.database.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.lifecycle.LiveData;
import com.example.ma2025.data.database.entities.TaskEntity;
import java.util.List;

@Dao
public interface TaskDao {

    @Insert
    long insertTask(TaskEntity task);

    @Update
    void updateTask(TaskEntity task);

    @Delete
    void deleteTask(TaskEntity task);

    @Query("SELECT * FROM tasks WHERE user_id = :userId ORDER BY created_at DESC")
    LiveData<List<TaskEntity>> getAllTasks(String userId);

    @Query("SELECT * FROM tasks WHERE user_id = :userId AND status = :status ORDER BY due_time ASC")
    LiveData<List<TaskEntity>> getTasksByStatus(String userId, int status);

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    LiveData<TaskEntity> getTaskById(long taskId);

    @Query("SELECT * FROM tasks WHERE user_id = :userId AND category_id = :categoryId")
    LiveData<List<TaskEntity>> getTasksByCategory(String userId, long categoryId);

    @Query("SELECT * FROM tasks WHERE user_id = :userId AND due_time BETWEEN :startTime AND :endTime")
    LiveData<List<TaskEntity>> getTasksForDateRange(String userId, long startTime, long endTime);

    @Query("SELECT * FROM tasks WHERE user_id = :userId AND status = 0 AND due_time <= :currentTime")
    List<TaskEntity> getOverdueTasks(String userId, long currentTime);

    @Query("SELECT * FROM tasks WHERE user_id = :userId AND is_repeating = 1 AND status != 3")
    List<TaskEntity> getRepeatingTasks(String userId);

    // FIXED: Use JOIN with task_completions table for completion_date
    @Query("SELECT COUNT(*) FROM tasks t " +
            "INNER JOIN task_completions tc ON t.id = tc.task_id " +
            "WHERE t.user_id = :userId AND t.difficulty = :difficulty " +
            "AND DATE(tc.completion_date/1000, 'unixepoch') = DATE(:date/1000, 'unixepoch')")
    int getCompletedTasksCountByDifficultyAndDate(String userId, int difficulty, long date);

    @Query("SELECT COUNT(*) FROM tasks t " +
            "INNER JOIN task_completions tc ON t.id = tc.task_id " +
            "WHERE t.user_id = :userId AND t.importance = :importance " +
            "AND DATE(tc.completion_date/1000, 'unixepoch') = DATE(:date/1000, 'unixepoch')")
    int getCompletedTasksCountByImportanceAndDate(String userId, int importance, long date);

    @Query("UPDATE tasks SET status = :newStatus, updated_at = :timestamp WHERE id = :taskId")
    void updateTaskStatus(long taskId, int newStatus, long timestamp);

    @Query("SELECT * FROM tasks WHERE synced_to_firebase = 0")
    List<TaskEntity> getUnsyncedTasks();

    @Query("UPDATE tasks SET synced_to_firebase = 1, firebase_id = :firebaseId WHERE id = :taskId")
    void markTaskAsSynced(long taskId, String firebaseId);

    @Query("SELECT COUNT(*) FROM tasks WHERE user_id = :userId AND status = 1")
    int getTotalCompletedTasks(String userId);

    @Query("SELECT COUNT(*) FROM tasks WHERE user_id = :userId")
    int getTotalTasks(String userId);

    // FIXED: Use JOIN with task_completions for completion dates
    @Query("SELECT COUNT(*) FROM tasks t " +
            "INNER JOIN task_completions tc ON t.id = tc.task_id " +
            "WHERE t.user_id = :userId " +
            "AND DATE(tc.completion_date/1000, 'unixepoch') = DATE(:date/1000, 'unixepoch')")
    int getTasksCompletedOnDate(String userId, long date);

    @Query("SELECT t.difficulty, COUNT(*) as count FROM tasks t " +
            "INNER JOIN task_completions tc ON t.id = tc.task_id " +
            "WHERE t.user_id = :userId GROUP BY t.difficulty")
    List<DifficultyCount> getCompletedTasksByDifficulty(String userId);

    @Query("SELECT t.category_id, COUNT(*) as count FROM tasks t " +
            "INNER JOIN task_completions tc ON t.id = tc.task_id " +
            "WHERE t.user_id = :userId AND t.category_id IS NOT NULL GROUP BY t.category_id")
    List<CategoryCount> getCompletedTasksByCategory(String userId);

    // Add method to get task synchronously (for internal repository use)
    @Query("SELECT * FROM tasks WHERE id = :taskId LIMIT 1")
    TaskEntity getTaskByIdSync(long taskId);

    // Add method to delete all tasks (for cleanup)
    @Query("DELETE FROM tasks WHERE user_id = :userId")
    void deleteAllUserTasks(String userId);

    // Get tasks for specific date (using updated_at for completed tasks)
    @Query("SELECT * FROM tasks WHERE user_id = :userId AND status = 1 " +
            "AND DATE(updated_at/1000, 'unixepoch') = DATE(:date/1000, 'unixepoch')")
    List<TaskEntity> getTasksCompletedOnDateList(String userId, long date);

    // Get active tasks for today
    @Query("SELECT * FROM tasks WHERE user_id = :userId AND status = 0 " +
            "AND DATE(due_time/1000, 'unixepoch') = DATE(:date/1000, 'unixepoch')")
    LiveData<List<TaskEntity>> getActiveTasksForDate(String userId, long date);

    @Query("SELECT COUNT(*) FROM tasks t " +
            "INNER JOIN task_completions tc ON t.id = tc.task_id " +
            "WHERE t.user_id = :userId AND t.difficulty = :difficulty " +
            "AND tc.completion_date BETWEEN :startTime AND :endTime")
    int getCompletedTasksCountByDifficultyAndDateRange(String userId, int difficulty, long startTime, long endTime);

    @Query("SELECT COUNT(*) FROM tasks t " +
            "INNER JOIN task_completions tc ON t.id = tc.task_id " +
            "WHERE t.user_id = :userId AND t.importance = :importance " +
            "AND tc.completion_date BETWEEN :startTime AND :endTime")
    int getCompletedTasksCountByImportanceAndDateRange(String userId, int importance, long startTime, long endTime);

    @Query("SELECT COUNT(*) FROM tasks WHERE user_id = :userId AND status = 2")
    int getTotalFailedTasks(String userId);

    @Query("SELECT COUNT(*) FROM tasks WHERE user_id = :userId AND status = 4")
    int getTotalPausedTasks(String userId);

    @Query("SELECT COUNT(*) FROM tasks WHERE user_id = :userId AND status = 3")
    int getTotalCanceledTasks(String userId);

    @Query("DELETE FROM tasks")
    void deleteAll();

    @Query("DELETE FROM tasks WHERE id = :taskId OR (parent_task_id = :taskId AND status != :completedStatus)")
    void deleteRecurringTaskAndFutureInstances(long taskId, int completedStatus);

    @Query("UPDATE tasks SET title = :title, description = :description, " +
            "difficulty = :difficulty, importance = :importance, " +
            "updated_at = strftime('%s', 'now') * 1000 " +
            "WHERE parent_task_id = :masterTaskId AND status IN (0, 4)")
    void updateFutureInstancesOfRecurringTask(long masterTaskId, String title,
                                              String description, int difficulty,
                                              int importance);

    @Query("SELECT * FROM tasks WHERE user_id = :userId AND status = " + TaskEntity.STATUS_ACTIVE +
            " AND due_time IS NOT NULL AND due_time < :expirationThreshold")
    List<TaskEntity> getExpiredActiveTasks(String userId, long expirationThreshold);

    @Query("SELECT * FROM tasks WHERE user_id = :userId " +
            "AND created_at >= :startTime AND created_at <= :endTime " +
            "ORDER BY created_at ASC")
    List<TaskEntity> getTasksCreatedInPeriod(String userId, long startTime, long endTime);

    // Inner classes for query results
    public static class DifficultyCount {
        public int difficulty;
        public int count;

        public DifficultyCount() {}
    }

    public static class CategoryCount {
        public long category_id;
        public int count;

        public CategoryCount() {}
    }
}