package com.agrodairy.management.service.impl;

import com.agrodairy.management.dao.NotificationDAO;
import com.agrodairy.management.dao.impl.NotificationDAOImpl;
import com.agrodairy.management.exception.ValidationException;
import com.agrodairy.management.model.Notification;
import com.agrodairy.management.model.enums.NotificationCategory;
import com.agrodairy.management.service.NotificationService;

import java.util.List;

public class NotificationServiceImpl implements NotificationService {
    private final NotificationDAO notificationDAO;

    public NotificationServiceImpl() {
        this.notificationDAO = new NotificationDAOImpl();
    }

    public NotificationServiceImpl(NotificationDAO notificationDAO) {
        this.notificationDAO = notificationDAO;
    }

    @Override
    public Notification sendNotification(Long userId, String title, String message, NotificationCategory category) {
        if (userId == null || userId <= 0) throw new ValidationException("User ID is required.");
        if (title == null || title.trim().isEmpty()) throw new ValidationException("Notification title is required.");
        if (message == null || message.trim().isEmpty()) throw new ValidationException("Notification message is required.");
        if (category == null) throw new ValidationException("Notification category is required.");

        Notification n = new Notification();
        n.setUserId(userId);
        n.setTitle(title);
        n.setMessage(message);
        n.setCategory(category);
        n.setRead(false);

        Long id = notificationDAO.insert(n);
        n.setNotificationId(id);
        return n;
    }

    @Override
    public List<Notification> getUserNotifications(Long userId) {
        if (userId == null || userId <= 0) throw new ValidationException("Invalid user ID.");
        return notificationDAO.findByUserId(userId);
    }

    @Override
    public List<Notification> getUnreadUserNotifications(Long userId) {
        if (userId == null || userId <= 0) throw new ValidationException("Invalid user ID.");
        return notificationDAO.findUnreadByUserId(userId);
    }

    @Override
    public boolean markAsRead(Long notificationId) {
        if (notificationId == null || notificationId <= 0) throw new ValidationException("Invalid notification ID.");
        return notificationDAO.markAsRead(notificationId);
    }

    @Override
    public boolean markAllAsReadForUser(Long userId) {
        if (userId == null || userId <= 0) throw new ValidationException("Invalid user ID.");
        return notificationDAO.markAllAsReadForUser(userId);
    }
}
