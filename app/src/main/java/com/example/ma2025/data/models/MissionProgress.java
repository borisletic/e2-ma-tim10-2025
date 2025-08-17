package com.example.ma2025.data.models;

public class MissionProgress {
    private String userId;
    private int storeVisits;
    private int successfulAttacks;
    private int easyTasksCompleted;
    private int hardTasksCompleted;
    private boolean noFailedTasks;
    private int messageDays;
    private int totalDamageDealt;

    public MissionProgress() {}

    public MissionProgress(String userId) {
        this.userId = userId;
        this.storeVisits = 0;
        this.successfulAttacks = 0;
        this.easyTasksCompleted = 0;
        this.hardTasksCompleted = 0;
        this.noFailedTasks = true;
        this.messageDays = 0;
        this.totalDamageDealt = 0;
    }

    // Getteri
    public String getUserId() { return userId; }
    public int getStoreVisits() { return storeVisits; }
    public int getSuccessfulAttacks() { return successfulAttacks; }
    public int getEasyTasksCompleted() { return easyTasksCompleted; }
    public int getHardTasksCompleted() { return hardTasksCompleted; }
    public boolean isNoFailedTasks() { return noFailedTasks; }
    public int getMessageDays() { return messageDays; }
    public int getTotalDamageDealt() { return totalDamageDealt; }

    // Setteri
    public void setUserId(String userId) { this.userId = userId; }
    public void setStoreVisits(int storeVisits) { this.storeVisits = storeVisits; }
    public void setSuccessfulAttacks(int successfulAttacks) { this.successfulAttacks = successfulAttacks; }
    public void setEasyTasksCompleted(int easyTasksCompleted) { this.easyTasksCompleted = easyTasksCompleted; }
    public void setHardTasksCompleted(int hardTasksCompleted) { this.hardTasksCompleted = hardTasksCompleted; }
    public void setNoFailedTasks(boolean noFailedTasks) { this.noFailedTasks = noFailedTasks; }
    public void setMessageDays(int messageDays) { this.messageDays = messageDays; }
    public void setTotalDamageDealt(int totalDamageDealt) { this.totalDamageDealt = totalDamageDealt; }

    // Utility metode
    public void incrementStoreVisits() {
        if (storeVisits < 5) {
            storeVisits++;
            totalDamageDealt += 2;
        }
    }

    public void incrementSuccessfulAttacks() {
        if (successfulAttacks < 10) {
            successfulAttacks++;
            totalDamageDealt += 2;
        }
    }

    public void incrementEasyTasks() {
        if (easyTasksCompleted < 10) {
            easyTasksCompleted++;
            totalDamageDealt += 1;
        }
    }

    public void incrementHardTasks() {
        if (hardTasksCompleted < 6) {
            hardTasksCompleted++;
            totalDamageDealt += 4;
        }
    }

    public void addMessageDay() {
        messageDays++;
        totalDamageDealt += 4;
    }

    public void taskFailed() {
        noFailedTasks = false;
    }

    public int calculateFinalBonus() {
        return noFailedTasks ? 10 : 0;
    }
}