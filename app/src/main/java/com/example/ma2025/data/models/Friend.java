package com.example.ma2025.data.models;

public class Friend {
    private String friendId;
    private String username;
    private String avatar;
    private int level;
    private String title;
    private int pp;
    private long addedTime;
    private FriendshipStatus status;

    public enum FriendshipStatus {
        PENDING, ACCEPTED, BLOCKED
    }

    public Friend() {}

    public Friend(String friendId, String username, String avatar, int level, String title, int pp) {
        this.friendId = friendId;
        this.username = username;
        this.avatar = avatar;
        this.level = level;
        this.title = title;
        this.pp = pp;
        this.addedTime = System.currentTimeMillis();
        this.status = FriendshipStatus.PENDING;
    }

    // Getteri
    public String getFriendId() { return friendId; }
    public String getUsername() { return username; }
    public String getAvatar() { return avatar; }
    public int getLevel() { return level; }
    public String getTitle() { return title; }
    public int getPp() { return pp; }
    public long getAddedTime() { return addedTime; }
    public FriendshipStatus getStatus() { return status; }

    // Setteri
    public void setFriendId(String friendId) { this.friendId = friendId; }
    public void setUsername(String username) { this.username = username; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public void setLevel(int level) { this.level = level; }
    public void setTitle(String title) { this.title = title; }
    public void setPp(int pp) { this.pp = pp; }
    public void setAddedTime(long addedTime) { this.addedTime = addedTime; }
    public void setStatus(FriendshipStatus status) { this.status = status; }
}