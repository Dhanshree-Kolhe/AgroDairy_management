package com.agrodairy.management.model;

import com.agrodairy.management.model.enums.NotificationCategory;

import java.sql.Timestamp;
import java.util.Objects;

/**
 * Domain entity representing System & Customer Notifications.
 */
public class Notification {
    private Long notificationId;
    private Long userId;
    private String title;
    private String message;
    private NotificationCategory category;
    private Boolean read;
    private Timestamp createdAt;

    public Notification() {}

    public Notification(Long notificationId, Long userId, String title, String message, NotificationCategory category, Boolean read) {
        this.notificationId = notificationId;
        this.userId = userId;
        this.title = title;
        this.message = message;
        this.category = category;
        this.read = read;
    }

    public Long getNotificationId() { return notificationId; }
    public void setNotificationId(Long notificationId) { this.notificationId = notificationId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public NotificationCategory getCategory() { return category; }
    public void setCategory(NotificationCategory category) { this.category = category; }

    public Boolean getRead() { return read; }
    public void setRead(Boolean read) { this.read = read; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Notification that = (Notification) o;
        return Objects.equals(notificationId, that.notificationId) && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(notificationId, userId);
    }

    @Override
    public String toString() {
        return "Notification{" +
                "notificationId=" + notificationId +
                ", userId=" + userId +
                ", title='" + title + '\'' +
                ", category=" + category +
                ", read=" + read +
                '}';
    }
}
