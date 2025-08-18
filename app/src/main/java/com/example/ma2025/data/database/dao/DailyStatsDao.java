// Fixed DailyStatsDao.java - Added missing methods
package com.example.ma2025.data.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import androidx.lifecycle.LiveData;
import com.example.ma2025.data.database.entities.DailyStatsEntity;
import java.util.List;

@Dao
public interface DailyStatsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdateDailyStats(DailyStatsEntity dailyStats);

    @Update
    void updateDailyStats(DailyStatsEntity dailyStats);

    @Query("SELECT * FROM daily_stats WHERE user_id = :userId AND date = :date LIMIT 1")
    DailyStatsEntity getDailyStats(String userId, long date);

    @Query("SELECT * FROM daily_stats WHERE user_id = :userId ORDER BY date DESC LIMIT 7")
    LiveData<List<DailyStatsEntity>> getLast7DaysStats(String userId);

    @Query("SELECT * FROM daily_stats WHERE user_id = :userId ORDER BY date DESC LIMIT 30")
    LiveData<List<DailyStatsEntity>> getLast30DaysStats(String userId);

    @Query("SELECT * FROM daily_stats WHERE user_id = :userId AND date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    List<DailyStatsEntity> getStatsForDateRange(String userId, long startDate, long endDate);

    @Query("SELECT SUM(total_xp_earned) FROM daily_stats WHERE user_id = :userId")
    int getTotalXpEarned(String userId);

    @Query("SELECT SUM(tasks_completed) FROM daily_stats WHERE user_id = :userId")
    int getTotalTasksCompleted(String userId);

    @Query("SELECT MAX(streak_count) FROM daily_stats WHERE user_id = :userId")
    int getLongestStreak(String userId);

    @Query("SELECT COUNT(*) FROM daily_stats WHERE user_id = :userId AND tasks_completed > 0")
    int getActiveDaysCount(String userId);

    // ADDED: Missing method for cleanup
    @Query("DELETE FROM daily_stats WHERE user_id = :userId")
    void deleteAllUserStats(String userId);

    // ADDED: Get today's stats
    @Query("SELECT * FROM daily_stats WHERE user_id = :userId " +
            "AND DATE(date/1000, 'unixepoch') = DATE(:todayTimestamp/1000, 'unixepoch') LIMIT 1")
    DailyStatsEntity getTodayStats(String userId, long todayTimestamp);

    // ADDED: Get stats for current week
    @Query("SELECT * FROM daily_stats WHERE user_id = :userId " +
            "AND date BETWEEN :startOfWeek AND :endOfWeek ORDER BY date ASC")
    List<DailyStatsEntity> getWeekStats(String userId, long startOfWeek, long endOfWeek);

    // ADDED: Get stats for current month
    @Query("SELECT * FROM daily_stats WHERE user_id = :userId " +
            "AND date BETWEEN :startOfMonth AND :endOfMonth ORDER BY date ASC")
    List<DailyStatsEntity> getMonthStats(String userId, long startOfMonth, long endOfMonth);
}