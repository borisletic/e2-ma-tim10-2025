package com.example.ma2025.data.models;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Friend {
    private String id;
    private String friendId;
    private String friendUsername;
    private String friendAvatar;
    private int friendLevel;
    private String friendTitle;
    private String status; // "pending", "accepted", "blocked"
    private Date createdAt;
    private Date updatedAt;

    public Friend() {}

    public Friend(String friendId, String friendUsername, String friendAvatar,
                  int friendLevel, String friendTitle) {
        this.friendId = friendId;
        this.friendUsername = friendUsername;
        this.friendAvatar = friendAvatar;
        this.friendLevel = friendLevel;
        this.friendTitle = friendTitle;
        this.status = "accepted";
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getFriendId() { return friendId; }
    public void setFriendId(String friendId) { this.friendId = friendId; }

    public String getFriendUsername() { return friendUsername; }
    public void setFriendUsername(String friendUsername) { this.friendUsername = friendUsername; }

    public String getFriendAvatar() { return friendAvatar; }
    public void setFriendAvatar(String friendAvatar) { this.friendAvatar = friendAvatar; }

    public int getFriendLevel() { return friendLevel; }
    public void setFriendLevel(int friendLevel) { this.friendLevel = friendLevel; }

    public String getFriendTitle() { return friendTitle; }
    public void setFriendTitle(String friendTitle) { this.friendTitle = friendTitle; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
    public Map<String, Object> toMap(String userId) {
        Map<String, Object> map = new HashMap<>();
        map.put("userId", userId);
        map.put("friendId", this.friendId);
        map.put("friendUsername", this.friendUsername);
        map.put("friendAvatar", this.friendAvatar);
        map.put("friendLevel", this.friendLevel);
        map.put("friendTitle", this.friendTitle);
        map.put("status", this.status);
        map.put("createdAt", this.createdAt);
        map.put("updatedAt", this.updatedAt);
        return map;
    }

}