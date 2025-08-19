package com.example.ma2025.data.models;

import java.util.Date;

public class AllianceMessage {
    private String id;
    private String allianceId;
    private String senderId;
    private String senderUsername;
    private String message;
    private Date timestamp;

    public AllianceMessage() {}

    public AllianceMessage(String allianceId, String senderId, String senderUsername, String message) {
        this.allianceId = allianceId;
        this.senderId = senderId;
        this.senderUsername = senderUsername;
        this.message = message;
        this.timestamp = new Date();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getAllianceId() { return allianceId; }
    public void setAllianceId(String allianceId) { this.allianceId = allianceId; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getSenderUsername() { return senderUsername; }
    public void setSenderUsername(String senderUsername) { this.senderUsername = senderUsername; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
}