package com.example.ma2025.data.models;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MissionProgress {
    private String userId;
    private int storeVisits;
    private int successfulAttacks;
    private int easyTasksCompleted;
    private int hardTasksCompleted;
    private boolean noFailedTasks;
    private List<String> messageDays; // Čuva datume kada su poslate poruke
    private int totalDamageDealt;

    public MissionProgress() {
        this.messageDays = new ArrayList<>();
    }

    public MissionProgress(String userId) {
        this.userId = userId;
        this.storeVisits = 0;
        this.successfulAttacks = 0;
        this.easyTasksCompleted = 0;
        this.hardTasksCompleted = 0;
        this.noFailedTasks = true;
        this.messageDays = new ArrayList<>();
        this.totalDamageDealt = 0;
    }

    // Getteri
    public String getUserId() { return userId; }
    public int getStoreVisits() { return storeVisits; }
    public int getSuccessfulAttacks() { return successfulAttacks; }
    public int getEasyTasksCompleted() { return easyTasksCompleted; }
    public int getHardTasksCompleted() { return hardTasksCompleted; }
    public boolean isNoFailedTasks() { return noFailedTasks; }
    public List<String> getMessageDays() { return messageDays; }
    public int getMessageDaysCount() { return messageDays.size(); }
    public int getTotalDamageDealt() { return totalDamageDealt; }

    // Setteri
    public void setUserId(String userId) { this.userId = userId; }
    public void setStoreVisits(int storeVisits) { this.storeVisits = storeVisits; }
    public void setSuccessfulAttacks(int successfulAttacks) { this.successfulAttacks = successfulAttacks; }
    public void setEasyTasksCompleted(int easyTasksCompleted) { this.easyTasksCompleted = easyTasksCompleted; }
    public void setHardTasksCompleted(int hardTasksCompleted) { this.hardTasksCompleted = hardTasksCompleted; }
    public void setNoFailedTasks(boolean noFailedTasks) { this.noFailedTasks = noFailedTasks; }
    public void setMessageDays(List<String> messageDays) { this.messageDays = messageDays; }
    public void setTotalDamageDealt(int totalDamageDealt) { this.totalDamageDealt = totalDamageDealt; }

    // Utility metode sa kvotama
    public boolean incrementStoreVisits() {
        if (storeVisits < 5) {
            storeVisits++;
            totalDamageDealt += 2;
            return true;
        }
        return false; // Kvota dosegnuta
    }

    public boolean incrementSuccessfulAttacks() {
        if (successfulAttacks < 10) {
            successfulAttacks++;
            totalDamageDealt += 2;
            return true;
        }
        return false; // Kvota dosegnuta
    }

    public boolean incrementEasyTasks() {
        if (easyTasksCompleted < 10) {
            easyTasksCompleted++;
            totalDamageDealt += 1;
            return true;
        }
        return false; // Kvota dosegnuta
    }

    public boolean incrementHardTasks() {
        if (hardTasksCompleted < 6) {
            hardTasksCompleted++;
            totalDamageDealt += 4;
            return true;
        }
        return false; // Kvota dosegnuta
    }

    public boolean addMessageDay(String date) {
        if (!messageDays.contains(date)) {
            messageDays.add(date);
            totalDamageDealt += 4;
            return true;
        }
        return false; // Dan već zabeležen
    }

    public void taskFailed() {
        noFailedTasks = false;
    }

    public int calculateFinalBonus() {
        return noFailedTasks ? 10 : 0;
    }

    // Metoda za proveru da li može da izvršava određenu akciju
    public boolean canPerformAction(String actionType) {
        switch (actionType) {
            case "store_visit":
                return storeVisits < 5;
            case "successful_attack":
                return successfulAttacks < 10;
            case "easy_task":
                return easyTasksCompleted < 10;
            case "hard_task":
                return hardTasksCompleted < 6;
            case "message_day":
                return true; // Nema ograničenja
            default:
                return false;
        }
    }

    // Metoda za dobijanje preostale kvote
    public int getRemainingQuota(String actionType) {
        switch (actionType) {
            case "store_visit":
                return Math.max(0, 5 - storeVisits);
            case "successful_attack":
                return Math.max(0, 10 - successfulAttacks);
            case "easy_task":
                return Math.max(0, 10 - easyTasksCompleted);
            case "hard_task":
                return Math.max(0, 6 - hardTasksCompleted);
            case "message_day":
                return Integer.MAX_VALUE;
            default:
                return 0;
        }
    }
}