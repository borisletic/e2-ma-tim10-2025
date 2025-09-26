package com.example.ma2025.data.models;

import com.example.ma2025.data.database.entities.TaskEntity;

import java.util.ArrayList;
import java.util.List;

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
    public int getMessageDaysCount() { return messageDays != null ? messageDays.size() : 0; }
    public int getTotalDamageDealt() { return totalDamageDealt; }

    // Setteri
    public void setUserId(String userId) { this.userId = userId; }
    public void setStoreVisits(int storeVisits) { this.storeVisits = storeVisits; }
    public void setSuccessfulAttacks(int successfulAttacks) { this.successfulAttacks = successfulAttacks; }
    public void setEasyTasksCompleted(int easyTasksCompleted) { this.easyTasksCompleted = easyTasksCompleted; }
    public void setHardTasksCompleted(int hardTasksCompleted) { this.hardTasksCompleted = hardTasksCompleted; }
    public void setNoFailedTasks(boolean noFailedTasks) { this.noFailedTasks = noFailedTasks; }
    public void setMessageDays(List<String> messageDays) {
        this.messageDays = messageDays != null ? messageDays : new ArrayList<>();
    }
    public void setTotalDamageDealt(int totalDamageDealt) { this.totalDamageDealt = totalDamageDealt; }

    // ========== GLAVNE METODE ZA SPECIJALNE ZADATKE ==========

    /**
     * Kupovina u prodavnici (max 5) - 2 HP
     */
    public boolean incrementStoreVisits() {
        if (storeVisits < 5) {
            storeVisits++;
            totalDamageDealt += 2;
            return true;
        }
        return false;
    }

    /**
     * Uspešan udarac u regularnoj borbi (max 10) - 2 HP
     */
    public boolean incrementSuccessfulAttacks() {
        if (successfulAttacks < 10) {
            successfulAttacks++;
            totalDamageDealt += 2;
            return true;
        }
        return false;
    }

    /**
     * Laki zadaci (max 10) - 1 HP ili 2 HP ako je "easy and normal"
     */
    public boolean incrementEasyTasks(boolean isEasyAndNormal) {
        if (easyTasksCompleted < 10) {
            int increment = isEasyAndNormal ? 2 : 1;
            int newValue = Math.min(easyTasksCompleted + increment, 10);
            int actualIncrement = newValue - easyTasksCompleted;

            easyTasksCompleted = newValue;
            totalDamageDealt += actualIncrement;
            return true;
        }
        return false;
    }

    /**
     * Teški zadaci (max 6) - 4 HP
     */
    public boolean incrementHardTasks() {
        if (hardTasksCompleted < 6) {
            hardTasksCompleted++;
            totalDamageDealt += 4;
            return true;
        }
        return false;
    }

    /**
     * Poruka u savezu (računa se po danu) - 4 HP
     */
    public boolean addMessageDay(String date) {
        if (messageDays == null) {
            messageDays = new ArrayList<>();
        }

        if (!messageDays.contains(date)) {
            messageDays.add(date);
            totalDamageDealt += 4;
            return true;
        }
        return false;
    }

    /**
     * Označava da je zadatak neuspešan
     */
    public void taskFailed() {
        noFailedTasks = false;
    }

    /**
     * Bonus za "bez neuspešnih zadataka" - 10 HP
     */
    public int calculateFinalBonus() {
        return noFailedTasks ? 10 : 0;
    }

    // ========== HELPER METODE ==========

    /**
     * Provera da li je zadatak "easy and normal" (Lak 3XP i Normalan 1XP)
     * Takvi zadaci se broje 2 puta
     */
    public static boolean isEasyAndNormal(int tezina, int bitnost) {
        // Umesto da proverava XP (3, 1), proveri konstante (2, 1)
        return (tezina == TaskEntity.DIFFICULTY_EASY && bitnost == TaskEntity.IMPORTANCE_NORMAL);
    }

    /**
     * Provera da li je zadatak "easy" (veoma lak, lak, normalan ili važan)
     */
    public static boolean isEasyTask(int tezina, int bitnost) {
        // Veoma lak (1) ili Lak (2) težina
        boolean isEasyDifficulty = (tezina == TaskEntity.DIFFICULTY_VERY_EASY ||
                tezina == TaskEntity.DIFFICULTY_EASY);
        // Normalan (1) ili Važan (2) bitnost
        boolean isNormalImportance = (bitnost == TaskEntity.IMPORTANCE_NORMAL ||
                bitnost == TaskEntity.IMPORTANCE_IMPORTANT);

        return isEasyDifficulty || isNormalImportance;
    }

    /**
     * Provera da li korisnik može izvršiti akciju (nije dostigao kvotu)
     */
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
                return true;
            default:
                return false;
        }
    }

    /**
     * Preostala kvota za određenu akciju
     */
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