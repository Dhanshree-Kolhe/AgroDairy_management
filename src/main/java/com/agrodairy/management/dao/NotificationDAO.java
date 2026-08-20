package com.agrodairy.management.dao;

import com.agrodairy.management.model.Notification;
import com.agrodairy.management.model.enums.NotificationCategory;

import java.util.List;
import java.util.Optional;

public interface NotificationDAO {
    Long insert(Notification notification);
    boolean markAsRead(Long notificationId);
    boolean markAllAsReadForUser(Long userId);
    Optional<Notification> findById(Long notificationId);
    List<Notification> findByUserId(Long userId);
    List<Notification> findUnreadByUserId(Long userId);
    List<Notification> findByCategory(NotificationCategory category);
}
