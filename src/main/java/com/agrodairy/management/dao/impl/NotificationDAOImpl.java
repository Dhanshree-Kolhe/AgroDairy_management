package com.agrodairy.management.dao.impl;

import com.agrodairy.management.dao.NotificationDAO;
import com.agrodairy.management.exception.DatabaseException;
import com.agrodairy.management.model.Notification;
import com.agrodairy.management.model.enums.NotificationCategory;
import com.agrodairy.management.util.DatabaseUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class NotificationDAOImpl implements NotificationDAO {

    @Override
    public Long insert(Notification n) {
        String sql = "INSERT INTO notifications (user_id, title, message, category, is_read) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, n.getUserId());
            stmt.setString(2, n.getTitle());
            stmt.setString(3, n.getMessage());
            stmt.setString(4, n.getCategory().name());
            stmt.setBoolean(5, n.getRead() != null ? n.getRead() : false);

            int affected = stmt.executeUpdate();
            if (affected == 0) throw new DatabaseException("Creating notification failed, no rows affected.");

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    long id = generatedKeys.getLong(1);
                    n.setNotificationId(id);
                    return id;
                } else {
                    throw new DatabaseException("Creating notification failed, no ID obtained.");
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to insert notification: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean markAsRead(Long notificationId) {
        String sql = "UPDATE notifications SET is_read = TRUE WHERE notification_id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, notificationId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to mark notification as read: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean markAllAsReadForUser(Long userId) {
        String sql = "UPDATE notifications SET is_read = TRUE WHERE user_id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to mark notifications as read for user: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Notification> findById(Long notificationId) {
        String sql = "SELECT * FROM notifications WHERE notification_id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, notificationId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapResultSetToNotification(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find notification by ID: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public List<Notification> findByUserId(Long userId) {
        List<Notification> list = new ArrayList<>();
        String sql = "SELECT * FROM notifications WHERE user_id = ? ORDER BY created_at DESC";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToNotification(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find notifications by user ID: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public List<Notification> findUnreadByUserId(Long userId) {
        List<Notification> list = new ArrayList<>();
        String sql = "SELECT * FROM notifications WHERE user_id = ? AND is_read = FALSE ORDER BY created_at DESC";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToNotification(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find unread notifications by user ID: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public List<Notification> findByCategory(NotificationCategory category) {
        List<Notification> list = new ArrayList<>();
        String sql = "SELECT * FROM notifications WHERE category = ? ORDER BY created_at DESC";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, category.name());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToNotification(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find notifications by category: " + e.getMessage(), e);
        }
        return list;
    }

    private Notification mapResultSetToNotification(ResultSet rs) throws SQLException {
        Notification n = new Notification();
        n.setNotificationId(rs.getLong("notification_id"));
        n.setUserId(rs.getLong("user_id"));
        n.setTitle(rs.getString("title"));
        n.setMessage(rs.getString("message"));
        n.setCategory(NotificationCategory.valueOf(rs.getString("category")));
        n.setRead(rs.getBoolean("is_read"));
        n.setCreatedAt(rs.getTimestamp("created_at"));
        return n;
    }
}
