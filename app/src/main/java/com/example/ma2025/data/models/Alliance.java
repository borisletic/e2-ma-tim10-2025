package com.example.ma2025.data.models;

import java.util.ArrayList;
import java.util.List;

public class Alliance {
    private String id;
    private String name;
    private String leaderId;
    private List<String> memberIds;
    private long createdTime;
    private boolean hasActiveMission;
    private SpecialMission currentMission;

    public Alliance() {
        this.memberIds = new ArrayList<>();
        this.hasActiveMission = false;
        this.createdTime = System.currentTimeMillis();
    }

    public Alliance(String id, String name, String leaderId) {
        this();
        this.id = id;
        this.name = name;
        this.leaderId = leaderId;
        this.memberIds.add(leaderId); // Lider je automatski član
    }

    // Getteri
    public String getId() { return id; }
    public String getName() { return name; }
    public String getLeaderId() { return leaderId; }
    public List<String> getMemberIds() { return memberIds; }
    public long getCreatedTime() { return createdTime; }
    public boolean hasActiveMission() { return hasActiveMission; }
    public SpecialMission getCurrentMission() { return currentMission; }

    // Setteri
    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setLeaderId(String leaderId) { this.leaderId = leaderId; }
    public void setMemberIds(List<String> memberIds) { this.memberIds = memberIds; }
    public void setCreatedTime(long createdTime) { this.createdTime = createdTime; }
    public void setHasActiveMission(boolean hasActiveMission) { this.hasActiveMission = hasActiveMission; }
    public void setCurrentMission(SpecialMission currentMission) { this.currentMission = currentMission; }

    // Utility metode
    public void addMember(String userId) {
        if (!memberIds.contains(userId)) {
            memberIds.add(userId);
        }
    }

    public void removeMember(String userId) {
        memberIds.remove(userId);
    }

    public boolean isLeader(String userId) {
        return leaderId.equals(userId);
    }

    public boolean isMember(String userId) {
        return memberIds.contains(userId);
    }

    public int getMemberCount() {
        return memberIds.size();
    }
}