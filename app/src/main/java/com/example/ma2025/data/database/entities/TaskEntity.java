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

    // Status constants
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

    // Grace period konstanta (3 dana)
    public static final long GRACE_PERIOD_MILLIS = 3 * 24 * 60 * 60 * 1000L;

    // ========== CONSTRUCTORS ==========

    public TaskEntity() {
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
        this.syncedToFirebase = false;
        this.status = STATUS_ACTIVE;
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

    // ========== XP CALCULATION ==========

    public int calculateXpValue(int userLevel) {
        int difficultyXp = getDifficultyXp(difficulty, userLevel);
        int importanceXp = getImportanceXp(importance, userLevel);
        return difficultyXp + importanceXp;
    }

    private int getDifficultyXp(int difficulty, int level) {
        int baseXp;
        switch (difficulty) {
            case DIFFICULTY_VERY_EASY: baseXp = 1; break;
            case DIFFICULTY_EASY: baseXp = 3; break;
            case DIFFICULTY_HARD: baseXp = 7; break;
            case DIFFICULTY_EXTREME: baseXp = 20; break;
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
            case IMPORTANCE_NORMAL: baseXp = 1; break;
            case IMPORTANCE_IMPORTANT: baseXp = 3; break;
            case IMPORTANCE_VERY_IMPORTANT: baseXp = 10; break;
            case IMPORTANCE_SPECIAL: baseXp = 100; break;
            default: baseXp = 1;
        }

        for (int i = 0; i < level; i++) {
            baseXp = (int) (baseXp + baseXp / 2.0);
        }
        return baseXp;
    }

    // ========== STATUS CHECKS ==========

    public boolean isActive() {
        return status == STATUS_ACTIVE;
    }

    public boolean isCompleted() {
        return status == STATUS_COMPLETED;
    }

    public boolean isFailed() {
        return status == STATUS_FAILED;
    }

    public boolean isCanceled() {
        return status == STATUS_CANCELED;
    }

    public boolean isPaused() {
        return status == STATUS_PAUSED;
    }

    // ========== GRACE PERIOD LOGIC ==========

    /**
     * Proverava da li je zadatak prosao grace period od 3 dana nakon due date
     */
    public boolean isExpired() {
        if (dueTime == null || status != STATUS_ACTIVE) {
            return false;
        }

        long now = System.currentTimeMillis();
        return (now - dueTime) > GRACE_PERIOD_MILLIS;
    }

    /**
     * Proverava da li je zadatak u grace periodu (prošao rok ali još uvek može da se uradi)
     */
    public boolean isInGracePeriod() {
        if (dueTime == null || status != STATUS_ACTIVE) {
            return false;
        }

        long now = System.currentTimeMillis();
        return (now > dueTime) && ((now - dueTime) <= GRACE_PERIOD_MILLIS);
    }

    /**
     * Proverava da li je zadatak prošao rok (bez obzira na grace period)
     */
    public boolean isOverdue() {
        if (dueTime == null || status != STATUS_ACTIVE) {
            return false;
        }

        return System.currentTimeMillis() > dueTime;
    }

    /**
     * Vraća broj dana koliko je zadatak u grace periodu (1-3)
     */
    public int getDaysInGracePeriod() {
        if (!isInGracePeriod()) {
            return 0;
        }

        long now = System.currentTimeMillis();
        long timeSinceDue = now - dueTime;
        return (int) (timeSinceDue / (24 * 60 * 60 * 1000L)) + 1;
    }

    /**
     * Vraća koliko dana ostaje do expiracije (0-3)
     */
    public int getDaysUntilExpiration() {
        if (dueTime == null || status != STATUS_ACTIVE) {
            return -1;
        }

        long now = System.currentTimeMillis();
        if (now <= dueTime) {
            return 3; // Još uvek nije prošao rok
        }

        long timeSinceDue = now - dueTime;
        long daysLeft = 3 - (timeSinceDue / (24 * 60 * 60 * 1000L));
        return Math.max(0, (int) daysLeft);
    }

    // ========== PERMISSION CHECKS ==========

    /**
     * Proverava da li zadatak može da se modifikuje
     * Neurađeni i otkazani zadaci se ne mogu menjati
     */
    public boolean canBeModified() {
        return status != STATUS_FAILED && status != STATUS_CANCELED;
    }

    /**
     * Proverava da li zadatak može da se briše
     * Neurađeni i otkazani zadaci se ne mogu brisati
     */
    public boolean canBeDeleted() {
        return status != STATUS_FAILED && status != STATUS_CANCELED;
    }

    /**
     * Proverava da li zadatak može da se označi kao završen
     * Samo aktivni zadaci koji nisu expired mogu da se završe
     */
    public boolean canBeCompleted() {
        if (status != STATUS_ACTIVE) {
            return false;
        }

        // Ako je expired, ne može se završiti
        return !isExpired();
    }

    /**
     * Proverava da li zadatak može da se pauzira
     */
    public boolean canBePaused() {
        return status == STATUS_ACTIVE && isRepeating;
    }

    /**
     * Proverava da li zadatak može da se nastavi (resume)
     */
    public boolean canBeResumed() {
        return status == STATUS_PAUSED;
    }

    // ========== XP ELIGIBILITY ==========

    /**
     * Proverava da li zadatak može da dobije XP kada se završi
     * Samo završeni zadaci dobijaju XP - pauzirani i otkazani ne
     */
    public boolean canEarnXp() {
        return status == STATUS_COMPLETED;
    }

    /**
     * Proverava da li je zadatak u stanju koje može da se završi za XP
     * Samo aktivni zadaci mogu da se završe i dobiju XP
     */
    public boolean isEligibleForXp() {
        return status == STATUS_ACTIVE;
    }

    // ========== STATUS CHANGES ==========

    public void markCompleted() {
        this.status = STATUS_COMPLETED;
        this.updatedAt = System.currentTimeMillis();
        this.syncedToFirebase = false;
    }

    public void markFailed() {
        this.status = STATUS_FAILED;
        this.updatedAt = System.currentTimeMillis();
        this.syncedToFirebase = false;
    }

    public void markCanceled() {
        this.status = STATUS_CANCELED;
        this.updatedAt = System.currentTimeMillis();
        this.syncedToFirebase = false;
    }

    public void pause() {
        if (canBePaused()) {
            this.status = STATUS_PAUSED;
            this.updatedAt = System.currentTimeMillis();
            this.syncedToFirebase = false;
        }
    }

    public void activate() {
        this.status = STATUS_ACTIVE;
        this.updatedAt = System.currentTimeMillis();
        this.syncedToFirebase = false;
    }

    /**
     * Automatski označi zadatak kao neurađen ako je expired
     */
    public boolean autoMarkAsFailedIfExpired() {
        if (isExpired()) {
            markFailed();
            return true;
        }
        return false;
    }

    // ========== EFFECTIVE STATUS ==========

    /**
     * Vraća efektivni status zadatka uzimajući u obzir grace period
     * Ova metoda ZAMENJUJE staru getEffectiveStatus()
     */
    public int getEffectiveStatus() {
        // Ako je zadatak već failed, completed, canceled ili paused - vrati trenutni status
        if (status != STATUS_ACTIVE) {
            return status;
        }

        // Ako nema due time, ostaje aktivan
        if (dueTime == null) {
            return STATUS_ACTIVE;
        }

        long now = System.currentTimeMillis();

        // Ako još uvek nije prošao rok
        if (now <= dueTime) {
            return STATUS_ACTIVE;
        }

        // Ako je prošao rok ali je u grace periodu
        if (isInGracePeriod()) {
            return STATUS_ACTIVE; // Još uvek može da se uradi
        }

        // Ako je prošao grace period
        return STATUS_FAILED;
    }

    /**
     * Pomoćna metoda koja vraća true ako je zadatak efektivno aktivan
     */
    public boolean isEffectivelyActive() {
        return getEffectiveStatus() == STATUS_ACTIVE;
    }

    /**
     * Pomoćna metoda koja vraća true ako je zadatak efektivno neurađen
     */
    public boolean isEffectivelyFailed() {
        return getEffectiveStatus() == STATUS_FAILED;
    }
}