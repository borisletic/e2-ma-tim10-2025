package com.example.ma2025.data.database.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;
import androidx.annotation.NonNull;

@Entity(tableName = "user_progress")
public class UserProgressEntity {
    @PrimaryKey
    @NonNull  // ✅ DODATO: Označava da primary key ne može biti null
    @ColumnInfo(name = "user_id")
    public String userId;

    @ColumnInfo(name = "current_level")
    public int currentLevel;

    @ColumnInfo(name = "current_xp")
    public int currentXp;

    @ColumnInfo(name = "total_pp")
    public int totalPp;

    @ColumnInfo(name = "coins")
    public int coins;

    @ColumnInfo(name = "current_streak")
    public int currentStreak;

    @ColumnInfo(name = "longest_streak")
    public int longestStreak;

    @ColumnInfo(name = "last_sync_timestamp")
    public long lastSyncTimestamp;

    @ColumnInfo(name = "updated_at")
    public long updatedAt;

    public UserProgressEntity() {
        this.userId = ""; // ✅ DODATO: Default vrednost umesto null
        this.currentLevel = 0;
        this.currentXp = 0;
        this.totalPp = 0;
        this.coins = 0;
        this.currentStreak = 0;
        this.longestStreak = 0;
        this.lastSyncTimestamp = 0;
        this.updatedAt = System.currentTimeMillis();
    }

    public UserProgressEntity(@NonNull String userId) {
        this();
        this.userId = userId;
    }

    public void addXp(int xp) {
        this.currentXp += xp;
        this.updatedAt = System.currentTimeMillis();
    }

    public void addCoins(int coins) {
        this.coins += coins;
        this.updatedAt = System.currentTimeMillis();
    }

    public void levelUp(int newLevel, int ppGained) {
        this.currentLevel = newLevel;
        this.totalPp += ppGained;
        this.updatedAt = System.currentTimeMillis();
    }

    public void updateStreak(int newStreak) {
        this.currentStreak = newStreak;
        if (newStreak > this.longestStreak) {
            this.longestStreak = newStreak;
        }
        this.updatedAt = System.currentTimeMillis();
    }
}