package com.example.ma2025.data.models;

import java.util.Date;

public class AllianceInvitation {
    private String id;
    private String allianceId;
    private String allianceName;
    private String fromUserId;
    private String fromUsername;
    private String toUserId;
    private String status; // "pending", "accepted", "declined"
    private Date createdAt;
    private Date respondedAt;

    public AllianceInvitation() {}

    public AllianceInvitation(String allianceId, String allianceName, String fromUserId,
                              String fromUsername, String toUserId) {
        this.allianceId = allianceId;
        this.allianceName = allianceName;
        this.fromUserId = fromUserId;
        this.fromUsername = fromUsername;
        this.toUserId = toUserId;
        this.status = "pending";
        this.createdAt = new Date();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getAllianceId() { return allianceId; }
    public void setAllianceId(String allianceId) { this.allianceId = allianceId; }

    public String getAllianceName() { return allianceName; }
    public void setAllianceName(String allianceName) { this.allianceName = allianceName; }

    public String getFromUserId() { return fromUserId; }
    public void setFromUserId(String fromUserId) { this.fromUserId = fromUserId; }

    public String getFromUsername() { return fromUsername; }
    public void setFromUsername(String fromUsername) { this.fromUsername = fromUsername; }

    public String getToUserId() { return toUserId; }
    public void setToUserId(String toUserId) { this.toUserId = toUserId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getRespondedAt() { return respondedAt; }
    public void setRespondedAt(Date respondedAt) { this.respondedAt = respondedAt; }
}