package com.example.ma2025.data.models;

import java.util.HashMap;
import java.util.Map;

public class SpecialMission {
    private String id;
    private String allianceId;
    private int bossHp;
    private int maxBossHp;
    private long startTime;
    private long endTime;
    private boolean isCompleted;
    private Map<String, MissionProgress> memberProgress;

    public SpecialMission() {
        this.memberProgress = new HashMap<>();
        this.isCompleted = false;
    }

    public SpecialMission(String id, String allianceId, int memberCount) {
        this();
        this.id = id;
        this.allianceId = allianceId;
        this.maxBossHp = 100 * memberCount;
        this.bossHp = maxBossHp;
        this.startTime = System.currentTimeMillis();
        this.endTime = startTime + (14 * 24 * 60 * 60 * 1000L); // 14 dana
    }

    // Getteri
    public String getId() { return id; }
    public String getAllianceId() { return allianceId; }
    public int getBossHp() { return bossHp; }
    public int getMaxBossHp() { return maxBossHp; }
    public long getStartTime() { return startTime; }
    public long getEndTime() { return endTime; }
    public boolean isCompleted() { return isCompleted; }
    public Map<String, MissionProgress> getMemberProgress() { return memberProgress; }

    // Setteri
    public void setId(String id) { this.id = id; }
    public void setAllianceId(String allianceId) { this.allianceId = allianceId; }
    public void setBossHp(int bossHp) { this.bossHp = bossHp; }
    public void setMaxBossHp(int maxBossHp) { this.maxBossHp = maxBossHp; }
    public void setStartTime(long startTime) { this.startTime = startTime; }
    public void setEndTime(long endTime) { this.endTime = endTime; }
    public void setCompleted(boolean completed) { isCompleted = completed; }
    public void setMemberProgress(Map<String, MissionProgress> memberProgress) { this.memberProgress = memberProgress; }

    // Utility metode
    public void dealDamage(int damage) {
        this.bossHp = Math.max(0, this.bossHp - damage);
        if (this.bossHp <= 0) {
            this.isCompleted = true;
        }
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > endTime;
    }

    public long getRemainingTime() {
        return Math.max(0, endTime - System.currentTimeMillis());
    }

    public void updateMemberProgress(String userId, MissionProgress progress) {
        memberProgress.put(userId, progress);
    }

    public double getProgressPercentage() {
        return ((double) (maxBossHp - bossHp) / maxBossHp) * 100;
    }
}