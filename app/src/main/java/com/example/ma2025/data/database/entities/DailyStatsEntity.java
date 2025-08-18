package com.example.ma2025.data.database.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;
import androidx.room.Index;
import androidx.annotation.NonNull;

@Entity(
        tableName = "daily_stats",
        indices = {@Index("user_id"), @Index("date")}
)
public class DailyStatsEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "user_id")
    @NonNull
    public String userId;

    @ColumnInfo(name = "date")
    public long date; // timestamp of day start (00:00:00)

    @ColumnInfo(name = "tasks_completed")
    public int tasksCompleted;

    @ColumnInfo(name = "tasks_failed")
    public int tasksFailed;

    @ColumnInfo(name = "total_xp_earned")
    public int totalXpEarned;

    @ColumnInfo(name = "streak_count")
    public int streakCount;

    @ColumnInfo(name = "very_easy_completed")
    public int veryEasyCompleted;

    @ColumnInfo(name = "easy_completed")
    public int easyCompleted;

    @ColumnInfo(name = "hard_completed")
    public int hardCompleted;

    @ColumnInfo(name = "extreme_completed")
    public int extremeCompleted;

    @ColumnInfo(name = "special_completed")
    public int specialCompleted;

    public DailyStatsEntity() {
        // Initialize all counters to 0
        this.tasksCompleted = 0;
        this.tasksFailed = 0;
        this.totalXpEarned = 0;
        this.streakCount = 0;
        this.veryEasyCompleted = 0;
        this.easyCompleted = 0;
        this.hardCompleted = 0;
        this.extremeCompleted = 0;
        this.specialCompleted = 0;
    }

    public DailyStatsEntity(String userId, long date) {
        this();
        this.userId = userId;
        this.date = date;
    }

    public void incrementTaskCompleted(int difficulty) {
        this.tasksCompleted++;
        switch (difficulty) {
            case TaskEntity.DIFFICULTY_VERY_EASY:
                this.veryEasyCompleted++;
                break;
            case TaskEntity.DIFFICULTY_EASY:
                this.easyCompleted++;
                break;
            case TaskEntity.DIFFICULTY_HARD:
                this.hardCompleted++;
                break;
            case TaskEntity.DIFFICULTY_EXTREME:
                this.extremeCompleted++;
                break;
        }
    }

    public void incrementTaskFailed() {
        this.tasksFailed++;
    }

    public void addXp(int xp) {
        this.totalXpEarned += xp;
    }
}