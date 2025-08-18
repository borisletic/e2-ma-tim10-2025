package com.example.ma2025.ui.levels.model;

public class LevelInfo {
    private int level;
    private int xpRequired;
    private int ppReward;
    private String title;
    private boolean isUnlocked;
    private boolean isCurrent;

    public LevelInfo() {}

    public LevelInfo(int level, int xpRequired, int ppReward, String title, boolean isUnlocked, boolean isCurrent) {
        this.level = level;
        this.xpRequired = xpRequired;
        this.ppReward = ppReward;
        this.title = title;
        this.isUnlocked = isUnlocked;
        this.isCurrent = isCurrent;
    }

    // Getters
    public int getLevel() { return level; }
    public int getXpRequired() { return xpRequired; }
    public int getPpReward() { return ppReward; }
    public String getTitle() { return title; }
    public boolean isUnlocked() { return isUnlocked; }
    public boolean isCurrent() { return isCurrent; }

    // Setters
    public void setLevel(int level) { this.level = level; }
    public void setXpRequired(int xpRequired) { this.xpRequired = xpRequired; }
    public void setPpReward(int ppReward) { this.ppReward = ppReward; }
    public void setTitle(String title) { this.title = title; }
    public void setUnlocked(boolean unlocked) { isUnlocked = unlocked; }
    public void setCurrent(boolean current) { isCurrent = current; }
}