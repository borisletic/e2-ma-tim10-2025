package com.example.ma2025.data.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import androidx.lifecycle.LiveData;
import com.example.ma2025.data.database.entities.UserProgressEntity;

@Dao
public interface UserProgressDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdateUserProgress(UserProgressEntity userProgress);

    @Update
    void updateUserProgress(UserProgressEntity userProgress);

    @Query("SELECT * FROM user_progress WHERE user_id = :userId")
    LiveData<UserProgressEntity> getUserProgress(String userId);

    @Query("SELECT * FROM user_progress WHERE user_id = :userId")
    UserProgressEntity getUserProgressSync(String userId);

    @Query("UPDATE user_progress SET current_xp = current_xp + :xp, updated_at = :timestamp WHERE user_id = :userId")
    void addXp(String userId, int xp, long timestamp);

    @Query("UPDATE user_progress SET coins = coins + :coins, updated_at = :timestamp WHERE user_id = :userId")
    void addCoins(String userId, int coins, long timestamp);

    @Query("UPDATE user_progress SET current_level = :level, total_pp = total_pp + :ppGained, updated_at = :timestamp WHERE user_id = :userId")
    void levelUp(String userId, int level, int ppGained, long timestamp);

    @Query("UPDATE user_progress SET current_streak = :streak, longest_streak = CASE WHEN :streak > longest_streak THEN :streak ELSE longest_streak END, updated_at = :timestamp WHERE user_id = :userId")
    void updateStreak(String userId, int streak, long timestamp);

    @Query("UPDATE user_progress SET last_sync_timestamp = :timestamp WHERE user_id = :userId")
    void updateLastSyncTime(String userId, long timestamp);

    @Query("DELETE FROM user_progress WHERE user_id = :userId")
    void deleteUserProgress(String userId);
}