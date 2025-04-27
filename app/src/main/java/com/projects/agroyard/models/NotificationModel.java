package com.projects.agroyard.models;
public class NotificationModel {
    private String title;

    private String notifiactionId;

    public String getNotifiactionId() {
        return notifiactionId;
    }

    private String message;  // Renamed to match Firebase "body"
    private long timestamp;  // Firebase uses long for timestamps
    private String colorCode = "#FFFFFF";  // Default color code (optional)
    private boolean isRead = false;  // Default read status (optional)

    // Default constructor required for Firebase
    public NotificationModel() {}

    public NotificationModel(String notifiactionId, String title, String message, long timestamp, String colorCode, boolean isRead) {
        this.title = title;
        this.message = message;
        this.timestamp = timestamp;
        this.colorCode = colorCode;
        this.isRead = isRead;
        this.notifiactionId = notifiactionId;
    }

    // Getters
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public long getTimestamp() { return timestamp; }
    public String getColorCode() { return colorCode; }
    public boolean isRead() { return isRead; }
}
