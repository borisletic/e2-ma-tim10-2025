package com.example.ma2025.data.models;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Alliance {
    private String id;
    private String name;
    private String leaderId;
    private String leaderUsername;
    private List<String> memberIds;
    private List<AllianceMember> members;
    private boolean missionActive;
    private Date createdAt;
    private Date updatedAt;

    public Alliance() {
        this.memberIds = new ArrayList<>();
        this.members = new ArrayList<>();
        this.missionActive = false;
    }

    public Alliance(String name, String leaderId, String leaderUsername) {
        this();
        this.name = name;
        this.leaderId = leaderId;
        this.leaderUsername = leaderUsername;
        this.createdAt = new Date();
        this.updatedAt = new Date();

        // Add leader as first member
        this.memberIds.add(leaderId);
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getLeaderId() { return leaderId; }
    public void setLeaderId(String leaderId) { this.leaderId = leaderId; }

    public String getLeaderUsername() { return leaderUsername; }
    public void setLeaderUsername(String leaderUsername) { this.leaderUsername = leaderUsername; }

    public List<String> getMemberIds() { return memberIds; }
    public void setMemberIds(List<String> memberIds) { this.memberIds = memberIds; }

    public List<AllianceMember> getMembers() { return members; }
    public void setMembers(List<AllianceMember> members) { this.members = members; }

    public boolean isMissionActive() { return missionActive; }
    public void setMissionActive(boolean missionActive) { this.missionActive = missionActive; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }

    public void addMember(String memberId) {
        if (!memberIds.contains(memberId)) {
            memberIds.add(memberId);
        }
    }

    public void removeMember(String memberId) {
        memberIds.remove(memberId);
    }

    public boolean isLeader(String userId) {
        return leaderId != null && leaderId.equals(userId);
    }

    public int getMemberCount() {
        return memberIds != null ? memberIds.size() : 0;
    }
}