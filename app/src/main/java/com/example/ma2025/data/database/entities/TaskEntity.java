package com.example.ma2025.data.database.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.annotation.NonNull;

@Entity(
        tableName = "tasks",
        foreignKeys = @ForeignKey(
                entity = CategoryEntity.class,
                parentColumns = "id",
                childColumns = "category_id",
                onDelete = ForeignKey.SET_NULL
        ),
        indices = {@Index("category_id"), @Index("user_id"), @Index("parent_task_id")}
)
public class TaskEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "user_id")
    @NonNull
    public String userId;

    @ColumnInfo(name = "title")
    public String title;

    @ColumnInfo(name = "description")
    public String description;

    @ColumnInfo(name = "category_id")
    public Long categoryId;

    @ColumnInfo(name = "difficulty")
    public int difficulty; // 1=very_easy, 2=easy, 3=hard, 4=extreme

    @ColumnInfo(name = "importance")
    public int importance; // 1=normal, 2=important, 3=very_important, 4=special

    @ColumnInfo(name = "is_repeating")
    public boolean isRepeating;

    @ColumnInfo(name = "parent_task_id")
    public Long parentTaskId;

    @ColumnInfo(name = "repeat_interval")
    public Integer repeatInterval;

    @ColumnInfo(name = "repeat_unit")
    public String repeatUnit; // 'day', 'week'

    @ColumnInfo(name = "start_date")
    public Long startDate;

    @ColumnInfo(name = "end_date")
    public Long endDate;

    @ColumnInfo(name = "due_time")
    public Long dueTime; // nullable, može biti null

    @ColumnInfo(name = "status")
    public int status; // 0=active, 1=completed, 2=failed, 3=canceled, 4=paused

    @ColumnInfo(name = "created_at")
    public long createdAt;

    @ColumnInfo(name = "updated_at")
    public long updatedAt;

    @ColumnInfo(name = "synced_to_firebase")
    public boolean syncedToFirebase;

    @ColumnInfo(name = "firebase_id")
    public String firebaseId; // ID u Firebase-u za sync

    // Constructors
    public TaskEntity() {
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
        this.syncedToFirebase = false;
        this.status = 0; // active by default
    }

    public TaskEntity(String userId, String title, String description, Long categoryId,
                      int difficulty, int importance) {
        this();
        this.userId = userId;
        this.title = title;
        this.description = description;
        this.categoryId = categoryId;
        this.difficulty = difficulty;
        this.importance = importance;
        this.isRepeating = false;
    }

    // Utility methods
    public int calculateXpValue(int userLevel) {
        int difficultyXp = getDifficultyXp(difficulty, userLevel);
        int importanceXp = getImportanceXp(importance, userLevel);
        return difficultyXp + importanceXp;
    }

    private int getDifficultyXp(int difficulty, int level) {
        int baseXp;
        switch (difficulty) {
            case 1: baseXp = 1; break;  // very easy
            case 2: baseXp = 3; break;  // easy
            case 3: baseXp = 7; break;  // hard
            case 4: baseXp = 20; break; // extreme
            default: baseXp = 1;
        }

        for (int i = 0; i < level; i++) {
            baseXp = (int) (baseXp + baseXp / 2.0);
        }
        return baseXp;
    }

    private int getImportanceXp(int importance, int level) {
        int baseXp;
        switch (importance) {
            case 1: baseXp = 1; break;   // normal
            case 2: baseXp = 3; break;   // important
            case 3: baseXp = 10; break;  // very important
            case 4: baseXp = 100; break; // special
            default: baseXp = 1;
        }

        for (int i = 0; i < level; i++) {
            baseXp = (int) (baseXp + baseXp / 2.0);
        }
        return baseXp;
    }

    public boolean isActive() {
        return status == 0;
    }

    public boolean isCompleted() {
        return status == 1;
    }

    public boolean isFailed() {
        return status == 2;
    }

    public boolean isCanceled() {
        return status == 3;
    }

    public boolean isPaused() {
        return status == 4;
    }

    public void markCompleted() {
        this.status = 1;
        this.updatedAt = System.currentTimeMillis();
        this.syncedToFirebase = false;
    }

    public void markFailed() {
        this.status = 2;
        this.updatedAt = System.currentTimeMillis();
        this.syncedToFirebase = false;
    }

    public void markCanceled() {
        this.status = 3;
        this.updatedAt = System.currentTimeMillis();
        this.syncedToFirebase = false;
    }

    public void pause() {
        if (this.isRepeating) { // samo ponavljajući zadaci mogu biti pauzirani
            this.status = 4;
            this.updatedAt = System.currentTimeMillis();
            this.syncedToFirebase = false;
        }
    }

    public void activate() {
        this.status = 0;
        this.updatedAt = System.currentTimeMillis();
        this.syncedToFirebase = false;
    }

    public int getEffectiveStatus() {
        long now = System.currentTimeMillis();

        if (this.status == STATUS_ACTIVE && this.dueTime != null && this.dueTime < now) {
            return STATUS_FAILED;
        }

        return this.status;
    }

    /**
     * Pomoćna metoda koja vraća true ako je zadatak efektivno aktivan
     */
    public boolean isEffectivelyActive() {
        return getEffectiveStatus() == STATUS_ACTIVE;
    }

    /**
     * Pomoćna metoda koja vraća true ako je zadatak efektivno neurađen (prošao rok)
     */
    public boolean isEffectivelyFailed() {
        return getEffectiveStatus() == STATUS_FAILED;
    }

    // Task status constants
    public static final int STATUS_ACTIVE = 0;
    public static final int STATUS_COMPLETED = 1;
    public static final int STATUS_FAILED = 2;
    public static final int STATUS_CANCELED = 3;
    public static final int STATUS_PAUSED = 4;

    // Difficulty constants
    public static final int DIFFICULTY_VERY_EASY = 1;
    public static final int DIFFICULTY_EASY = 2;
    public static final int DIFFICULTY_HARD = 3;
    public static final int DIFFICULTY_EXTREME = 4;

    // Importance constants
    public static final int IMPORTANCE_NORMAL = 1;
    public static final int IMPORTANCE_IMPORTANT = 2;
    public static final int IMPORTANCE_VERY_IMPORTANT = 3;
    public static final int IMPORTANCE_SPECIAL = 4;
}
