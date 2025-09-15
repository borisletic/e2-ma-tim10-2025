package com.example.ma2025.data.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.ma2025.data.database.entities.BossEntity;

import java.util.List;

@Dao
public interface BossDao {
    @Insert
    long insertBoss(BossEntity boss);

    @Update
    void updateBoss(BossEntity boss);

    @Query("SELECT * FROM bosses WHERE user_id = :userId AND level = :level")
    LiveData<BossEntity> getBossForLevel(String userId, int level);

    @Query("SELECT * FROM bosses WHERE user_id = :userId ORDER BY level ASC")
    LiveData<List<BossEntity>> getAllBossesForUser(String userId);

    @Query("DELETE FROM bosses WHERE user_id = :userId AND level = :level")
    void deleteBoss(String userId, int level);
}