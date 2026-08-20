package com.agrodairy.management.dao.impl;

import com.agrodairy.management.dao.ExtraOrderDAO;
import com.agrodairy.management.exception.DatabaseException;
import com.agrodairy.management.model.ExtraOrder;
import com.agrodairy.management.model.enums.RequestStatus;
import com.agrodairy.management.util.DatabaseUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ExtraOrderDAOImpl implements ExtraOrderDAO {

    @Override
    public Long insert(ExtraOrder order) {
        String sql = "INSERT INTO extra_orders (customer_id, product_id, quantity, price_snapshot, total_price, order_date, delivery_date, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, order.getCustomerId());
            stmt.setLong(2, order.getProductId());
            stmt.setBigDecimal(3, order.getQuantity());
            stmt.setBigDecimal(4, order.getPriceSnapshot()); // PRICE SNAPSHOT
            stmt.setBigDecimal(5, order.getTotalPrice());
            stmt.setDate(6, order.getOrderDate());
            stmt.setDate(7, order.getDeliveryDate());
            stmt.setString(8, order.getStatus() != null ? order.getStatus().name() : RequestStatus.PENDING.name());

            int affected = stmt.executeUpdate();
            if (affected == 0) throw new DatabaseException("Creating extra order failed, no rows affected.");

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    long id = generatedKeys.getLong(1);
                    order.setOrderId(id);
                    return id;
                } else {
                    throw new DatabaseException("Creating extra order failed, no ID obtained.");
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to insert extra order: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean updateStatus(Long orderId, RequestStatus status) {
        String sql = "UPDATE extra_orders SET status = ? WHERE order_id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status.name());
            stmt.setLong(2, orderId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update extra order status: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<ExtraOrder> findById(Long orderId) {
        String sql = "SELECT * FROM extra_orders WHERE order_id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, orderId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapResultSetToExtraOrder(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find extra order by ID: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public List<ExtraOrder> findByCustomerId(Long customerId) {
        List<ExtraOrder> orders = new ArrayList<>();
        String sql = "SELECT * FROM extra_orders WHERE customer_id = ? ORDER BY order_date DESC";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, customerId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    orders.add(mapResultSetToExtraOrder(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find extra orders by customer ID: " + e.getMessage(), e);
        }
        return orders;
    }

    @Override
    public List<ExtraOrder> findByDeliveryDate(Date deliveryDate) {
        List<ExtraOrder> orders = new ArrayList<>();
        String sql = "SELECT * FROM extra_orders WHERE delivery_date = ? ORDER BY order_id ASC";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDate(1, deliveryDate);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    orders.add(mapResultSetToExtraOrder(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find extra orders by delivery date: " + e.getMessage(), e);
        }
        return orders;
    }

    @Override
    public List<ExtraOrder> findPendingOrders() {
        List<ExtraOrder> orders = new ArrayList<>();
        String sql = "SELECT * FROM extra_orders WHERE status = 'PENDING' ORDER BY order_date ASC";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                orders.add(mapResultSetToExtraOrder(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find pending extra orders: " + e.getMessage(), e);
        }
        return orders;
    }

    @Override
    public List<ExtraOrder> findByCustomerAndMonth(Long customerId, int month, int year) {
        List<ExtraOrder> orders = new ArrayList<>();
        String sql = "SELECT * FROM extra_orders WHERE customer_id = ? AND MONTH(delivery_date) = ? AND YEAR(delivery_date) = ? AND status IN ('APPROVED', 'DELIVERED') ORDER BY delivery_date ASC";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, customerId);
            stmt.setInt(2, month);
            stmt.setInt(3, year);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    orders.add(mapResultSetToExtraOrder(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find extra orders by customer and month: " + e.getMessage(), e);
        }
        return orders;
    }

    private ExtraOrder mapResultSetToExtraOrder(ResultSet rs) throws SQLException {
        ExtraOrder eo = new ExtraOrder();
        eo.setOrderId(rs.getLong("order_id"));
        eo.setCustomerId(rs.getLong("customer_id"));
        eo.setProductId(rs.getLong("product_id"));
        eo.setQuantity(rs.getBigDecimal("quantity"));
        eo.setPriceSnapshot(rs.getBigDecimal("price_snapshot"));
        eo.setTotalPrice(rs.getBigDecimal("total_price"));
        eo.setOrderDate(rs.getDate("order_date"));
        eo.setDeliveryDate(rs.getDate("delivery_date"));
        eo.setStatus(RequestStatus.valueOf(rs.getString("status")));
        eo.setCreatedAt(rs.getTimestamp("created_at"));
        return eo;
    }
}
