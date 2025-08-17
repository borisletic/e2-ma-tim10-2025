package com.example.ma2025.data.models;

import java.util.ArrayList;
import java.util.List;

public class User {
    private String uid;
    private String email;
    private String username;
    private String avatar;
    private int level;
    private String title;
    private int xp;
    private int pp;
    private int coins;
    private List<String> badges;
    private boolean isActivated;
    private long registrationTime;
    private int activeDays;
    private int totalTasksCreated;
    private int totalTasksCompleted;
    private int totalTasksSkipped;
    private int totalTasksCanceled;
    private int longestStreak;
    private int currentStreak;

    public User() {
        this.badges = new ArrayList<>();
        this.level = 0;
        this.title = "Novajlija"; // Početna titula
        this.xp = 0;
        this.pp = 0;
        this.coins = 0;
        this.isActivated = false;
        this.activeDays = 0;
        this.totalTasksCreated = 0;
        this.totalTasksCompleted = 0;
        this.totalTasksSkipped = 0;
        this.totalTasksCanceled = 0;
        this.longestStreak = 0;
        this.currentStreak = 0;
        this.registrationTime = System.currentTimeMillis();
    }

    public User(String uid, String email, String username, String avatar) {
        this();
        this.uid = uid;
        this.email = email;
        this.username = username;
        this.avatar = avatar;
    }

    // Getteri
    public String getUid() { return uid; }
    public String getEmail() { return email; }
    public String getUsername() { return username; }
    public String getAvatar() { return avatar; }
    public int getLevel() { return level; }
    public String getTitle() { return title; }
    public int getXp() { return xp; }
    public int getPp() { return pp; }
    public int getCoins() { return coins; }
    public List<String> getBadges() { return badges; }
    public boolean isActivated() { return isActivated; }
    public long getRegistrationTime() { return registrationTime; }
    public int getActiveDays() { return activeDays; }
    public int getTotalTasksCreated() { return totalTasksCreated; }
    public int getTotalTasksCompleted() { return totalTasksCompleted; }
    public int getTotalTasksSkipped() { return totalTasksSkipped; }
    public int getTotalTasksCanceled() { return totalTasksCanceled; }
    public int getLongestStreak() { return longestStreak; }
    public int getCurrentStreak() { return currentStreak; }

    // Setteri
    public void setUid(String uid) { this.uid = uid; }
    public void setEmail(String email) { this.email = email; }
    public void setUsername(String username) { this.username = username; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public void setLevel(int level) { this.level = level; }
    public void setTitle(String title) { this.title = title; }
    public void setXp(int xp) { this.xp = xp; }
    public void setPp(int pp) { this.pp = pp; }
    public void setCoins(int coins) { this.coins = coins; }
    public void setBadges(List<String> badges) { this.badges = badges; }
    public void setActivated(boolean activated) { isActivated = activated; }
    public void setRegistrationTime(long registrationTime) { this.registrationTime = registrationTime; }
    public void setActiveDays(int activeDays) { this.activeDays = activeDays; }
    public void setTotalTasksCreated(int totalTasksCreated) { this.totalTasksCreated = totalTasksCreated; }
    public void setTotalTasksCompleted(int totalTasksCompleted) { this.totalTasksCompleted = totalTasksCompleted; }
    public void setTotalTasksSkipped(int totalTasksSkipped) { this.totalTasksSkipped = totalTasksSkipped; }
    public void setTotalTasksCanceled(int totalTasksCanceled) { this.totalTasksCanceled = totalTasksCanceled; }
    public void setLongestStreak(int longestStreak) { this.longestStreak = longestStreak; }
    public void setCurrentStreak(int currentStreak) { this.currentStreak = currentStreak; }

    // Utility metode
    public void addBadge(String badge) {
        if (!badges.contains(badge)) {
            badges.add(badge);
        }
    }

    public void addXp(int xpToAdd) {
        this.xp += xpToAdd;
    }

    public void addCoins(int coinsToAdd) {
        this.coins += coinsToAdd;
    }

    public int getXpForNextLevel() {
        if (level == 0) return 200;

        int xpForCurrentLevel = 200;
        for (int i = 1; i <= level; i++) {
            xpForCurrentLevel = (int) Math.ceil((xpForCurrentLevel * 2 + xpForCurrentLevel / 2.0) / 100.0) * 100;
        }
        return xpForCurrentLevel;
    }

    public boolean canLevelUp() {
        return xp >= getXpForNextLevel();
    }

    public void levelUp() {
        if (canLevelUp()) {
            level++;
            updateTitle();
            addPpForLevel();
        }
    }

    private void updateTitle() {
        switch (level) {
            case 1:
                title = "Početnik";
                break;
            case 2:
                title = "Istraživač";
                break;
            case 3:
                title = "Ratnik";
                break;
            case 4:
                title = "Veteran";
                break;
            case 5:
                title = "Majstor";
                break;
            default:
                title = "Legenda (Nivo " + level + ")";
                break;
        }
    }

    private void addPpForLevel() {
        if (level == 1) {
            pp += 40;
        } else {
            int previousPp = 40;
            for (int i = 2; i <= level; i++) {
                previousPp = (int) (previousPp + 0.75 * previousPp);
            }
            pp += previousPp;
        }
    }
}