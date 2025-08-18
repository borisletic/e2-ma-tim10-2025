// Fixed User.java - Added null safety and better error handling
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
        // Initialize with safe default values
        this.badges = new ArrayList<>();
        this.level = 0;
        this.title = "Novajlija"; // Default title
        this.xp = 0;
        this.pp = 0;
        this.coins = 0;
        this.isActivated = false;
        this.activeDays = 1; // Start with 1 day
        this.totalTasksCreated = 0;
        this.totalTasksCompleted = 0;
        this.totalTasksSkipped = 0;
        this.totalTasksCanceled = 0;
        this.longestStreak = 0;
        this.currentStreak = 0;
        this.registrationTime = System.currentTimeMillis();

        // Set safe defaults for strings
        this.uid = "";
        this.email = "";
        this.username = "";
        this.avatar = "avatar_1";
    }

    public User(String uid, String email, String username, String avatar) {
        this();
        this.uid = uid != null ? uid : "";
        this.email = email != null ? email : "";
        this.username = username != null ? username : "";
        this.avatar = avatar != null ? avatar : "avatar_1";
    }

    // Getters with null safety
    public String getUid() {
        return uid != null ? uid : "";
    }

    public String getEmail() {
        return email != null ? email : "";
    }

    public String getUsername() {
        return username != null ? username : "";
    }

    public String getAvatar() {
        return avatar != null ? avatar : "avatar_1";
    }

    public int getLevel() { return level; }

    public String getTitle() {
        return title != null ? title : "Novajlija";
    }

    public int getXp() { return xp; }
    public int getPp() { return pp; }
    public int getCoins() { return coins; }

    public List<String> getBadges() {
        return badges != null ? badges : new ArrayList<>();
    }

    public boolean isActivated() { return isActivated; }
    public long getRegistrationTime() { return registrationTime; }
    public int getActiveDays() { return Math.max(1, activeDays); } // Ensure at least 1
    public int getTotalTasksCreated() { return totalTasksCreated; }
    public int getTotalTasksCompleted() { return totalTasksCompleted; }
    public int getTotalTasksSkipped() { return totalTasksSkipped; }
    public int getTotalTasksCanceled() { return totalTasksCanceled; }
    public int getLongestStreak() { return longestStreak; }
    public int getCurrentStreak() { return currentStreak; }

    // Setters with null safety
    public void setUid(String uid) {
        this.uid = uid != null ? uid : "";
    }

    public void setEmail(String email) {
        this.email = email != null ? email : "";
    }

    public void setUsername(String username) {
        this.username = username != null ? username : "";
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar != null ? avatar : "avatar_1";
    }

    public void setLevel(int level) {
        this.level = Math.max(0, level); // Ensure non-negative
    }

    public void setTitle(String title) {
        this.title = title != null ? title : "Novajlija";
    }

    public void setXp(int xp) {
        this.xp = Math.max(0, xp); // Ensure non-negative
    }

    public void setPp(int pp) {
        this.pp = Math.max(0, pp); // Ensure non-negative
    }

    public void setCoins(int coins) {
        this.coins = Math.max(0, coins); // Ensure non-negative
    }

    public void setBadges(List<String> badges) {
        this.badges = badges != null ? badges : new ArrayList<>();
    }

    public void setActivated(boolean activated) {
        isActivated = activated;
    }

    public void setRegistrationTime(long registrationTime) {
        this.registrationTime = registrationTime > 0 ? registrationTime : System.currentTimeMillis();
    }

    public void setActiveDays(int activeDays) {
        this.activeDays = Math.max(1, activeDays); // Ensure at least 1
    }

    public void setTotalTasksCreated(int totalTasksCreated) {
        this.totalTasksCreated = Math.max(0, totalTasksCreated);
    }

    public void setTotalTasksCompleted(int totalTasksCompleted) {
        this.totalTasksCompleted = Math.max(0, totalTasksCompleted);
    }

    public void setTotalTasksSkipped(int totalTasksSkipped) {
        this.totalTasksSkipped = Math.max(0, totalTasksSkipped);
    }

    public void setTotalTasksCanceled(int totalTasksCanceled) {
        this.totalTasksCanceled = Math.max(0, totalTasksCanceled);
    }

    public void setLongestStreak(int longestStreak) {
        this.longestStreak = Math.max(0, longestStreak);
    }

    public void setCurrentStreak(int currentStreak) {
        this.currentStreak = Math.max(0, currentStreak);
    }

    // Utility methods with null safety
    public void addBadge(String badge) {
        if (badge != null && !badge.trim().isEmpty()) {
            if (badges == null) {
                badges = new ArrayList<>();
            }
            if (!badges.contains(badge)) {
                badges.add(badge);
            }
        }
    }

    public void addXp(int xpToAdd) {
        if (xpToAdd > 0) {
            this.xp += xpToAdd;
        }
    }

    public void addCoins(int coinsToAdd) {
        if (coinsToAdd > 0) {
            this.coins += coinsToAdd;
        }
    }

    public int getXpForNextLevel() {
        if (level == 0) return 200;

        try {
            int xpForCurrentLevel = 200;
            for (int i = 1; i <= level; i++) {
                xpForCurrentLevel = (int) Math.ceil((xpForCurrentLevel * 2 + xpForCurrentLevel / 2.0) / 100.0) * 100;

                // Prevent overflow
                if (xpForCurrentLevel < 0) {
                    return Integer.MAX_VALUE;
                }
            }
            return xpForCurrentLevel;
        } catch (Exception e) {
            // Fallback calculation
            return 200 * (level + 1);
        }
    }

    public boolean canLevelUp() {
        try {
            return xp >= getXpForNextLevel();
        } catch (Exception e) {
            return false;
        }
    }

    public void levelUp() {
        try {
            if (canLevelUp()) {
                level++;
                updateTitle();
                addPpForLevel();
            }
        } catch (Exception e) {
            // Log error but don't crash
            android.util.Log.e("User", "Error during level up", e);
        }
    }

    private void updateTitle() {
        try {
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
                    title = level > 5 ? "Legenda (Nivo " + level + ")" : "Novajlija";
                    break;
            }
        } catch (Exception e) {
            title = "Novajlija"; // Fallback
        }
    }

    private void addPpForLevel() {
        try {
            if (level == 1) {
                pp += 40;
            } else if (level > 1) {
                int previousPp = 40;
                for (int i = 2; i <= level; i++) {
                    previousPp = (int) (previousPp + 0.75 * previousPp);

                    // Prevent overflow
                    if (previousPp < 0) {
                        previousPp = Integer.MAX_VALUE / 2;
                        break;
                    }
                }
                pp += previousPp;
            }
        } catch (Exception e) {
            // Fallback: add basic PP
            pp += 40;
        }
    }

    // Validation method
    public boolean isValid() {
        return uid != null && !uid.trim().isEmpty() &&
                email != null && !email.trim().isEmpty() &&
                username != null && !username.trim().isEmpty();
    }

    // Create a safe copy of this user
    public User createSafeCopy() {
        User copy = new User();
        copy.setUid(this.getUid());
        copy.setEmail(this.getEmail());
        copy.setUsername(this.getUsername());
        copy.setAvatar(this.getAvatar());
        copy.setLevel(this.getLevel());
        copy.setTitle(this.getTitle());
        copy.setXp(this.getXp());
        copy.setPp(this.getPp());
        copy.setCoins(this.getCoins());
        copy.setBadges(new ArrayList<>(this.getBadges()));
        copy.setActivated(this.isActivated());
        copy.setRegistrationTime(this.getRegistrationTime());
        copy.setActiveDays(this.getActiveDays());
        copy.setTotalTasksCreated(this.getTotalTasksCreated());
        copy.setTotalTasksCompleted(this.getTotalTasksCompleted());
        copy.setTotalTasksSkipped(this.getTotalTasksSkipped());
        copy.setTotalTasksCanceled(this.getTotalTasksCanceled());
        copy.setLongestStreak(this.getLongestStreak());
        copy.setCurrentStreak(this.getCurrentStreak());
        return copy;
    }
}