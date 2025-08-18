package com.example.ma2025.data.database.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;
import androidx.room.ForeignKey;
import androidx.room.Index;

@Entity(
        tableName = "task_completions",
        foreignKeys = @ForeignKey(
                entity = TaskEntity.class,
                parentColumns = "id",
                childColumns = "task_id",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {@Index("task_id"), @Index("completion_date")}
)
public class TaskCompletionEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "task_id")
    public long taskId;

    @ColumnInfo(name = "completion_date")
    public long completionDate; // timestamp when completed

    @ColumnInfo(name = "xp_earned")
    public int xpEarned;

    @ColumnInfo(name = "created_at")
    public long createdAt;

    public TaskCompletionEntity() {
        this.createdAt = System.currentTimeMillis();
        this.completionDate = System.currentTimeMillis();
    }

    public TaskCompletionEntity(long taskId, int xpEarned) {
        this();
        this.taskId = taskId;
        this.xpEarned = xpEarned;
    }
}