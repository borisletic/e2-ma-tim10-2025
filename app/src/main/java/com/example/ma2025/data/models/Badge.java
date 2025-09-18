package com.example.ma2025.data.models;

public class Badge {
    private String id;
    private String userId;
    private String type;
    private int taskCount;
    private long earnedDate;
    private String description;

    public Badge() {}

    public Badge(String userId, String type, int taskCount, String description) {
        this.userId = userId;
        this.type = type;
        this.taskCount = taskCount;
        this.description = description;
        this.earnedDate = System.currentTimeMillis();
    }

    // Getteri
    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getType() {
        return type;
    }

    public int getTaskCount() {
        return taskCount;
    }

    public long getEarnedDate() {
        return earnedDate;
    }

    public String getDescription() {
        return description;
    }

    // Setteri
    public void setId(String id) {
        this.id = id;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setTaskCount(int taskCount) {
        this.taskCount = taskCount;
    }

    public void setEarnedDate(long earnedDate) {
        this.earnedDate = earnedDate;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}