package com.example.ma2025.data.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.lifecycle.LiveData;
import com.example.ma2025.data.database.entities.TaskCompletionEntity;
import java.util.List;

@Dao
public interface TaskCompletionDao {

    @Insert
    long insertTaskCompletion(TaskCompletionEntity completion);

    @Query("SELECT * FROM task_completions WHERE task_id = :taskId ORDER BY completion_date DESC")
    LiveData<List<TaskCompletionEntity>> getCompletionsForTask(long taskId);

    @Query("SELECT * FROM task_completions tc INNER JOIN tasks t ON tc.task_id = t.id WHERE t.user_id = :userId AND DATE(tc.completion_date/1000, 'unixepoch') = DATE(:date/1000, 'unixepoch')")
    List<TaskCompletionEntity> getCompletionsForDate(String userId, long date);

    @Query("SELECT SUM(xp_earned) FROM task_completions tc INNER JOIN tasks t ON tc.task_id = t.id WHERE t.user_id = :userId AND DATE(tc.completion_date/1000, 'unixepoch') = DATE(:date/1000, 'unixepoch')")
    int getTotalXpForDate(String userId, long date);

    @Query("SELECT COUNT(*) FROM task_completions tc INNER JOIN tasks t ON tc.task_id = t.id WHERE t.user_id = :userId AND tc.completion_date BETWEEN :startDate AND :endDate")
    int getCompletionsCount(String userId, long startDate, long endDate);

    @Query("SELECT * FROM task_completions tc INNER JOIN tasks t ON tc.task_id = t.id WHERE t.user_id = :userId ORDER BY tc.completion_date DESC LIMIT :limit")
    LiveData<List<TaskCompletionEntity>> getRecentCompletions(String userId, int limit);

    @Query("DELETE FROM task_completions WHERE task_id = :taskId")
    void deleteCompletionsForTask(long taskId);
}