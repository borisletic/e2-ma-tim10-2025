package com.example.ma2025.data.models;

import java.util.Date;

public class AllianceMember {
    private String userId;
    private String username;
    private String avatar;
    private int level;
    private String title;
    private String role; // "leader", "member"
    private Date joinedAt;

    public AllianceMember() {}

    public AllianceMember(String userId, String username, String avatar,
                          int level, String title, String role) {
        this.userId = userId;
        this.username = username;
        this.avatar = avatar;
        this.level = level;
        this.title = title;
        this.role = role;
        this.joinedAt = new Date();
    }

    // Getters and Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }

    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public Date getJoinedAt() { return joinedAt; }
    public void setJoinedAt(Date joinedAt) { this.joinedAt = joinedAt; }
}