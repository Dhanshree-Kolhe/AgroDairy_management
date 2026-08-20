package com.agrodairy.management.service;

import com.agrodairy.management.model.Notification;
import com.agrodairy.management.model.enums.NotificationCategory;

import java.util.List;

public interface NotificationService {
    Notification sendNotification(Long userId, String title, String message, NotificationCategory category);
    List<Notification> getUserNotifications(Long userId);
    List<Notification> getUnreadUserNotifications(Long userId);
    boolean markAsRead(Long notificationId);
    boolean markAllAsReadForUser(Long userId);
}
