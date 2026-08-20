package com.agrodairy.management.dao.impl;

import com.agrodairy.management.dao.DeliveryRecordDAO;
import com.agrodairy.management.exception.DatabaseException;
import com.agrodairy.management.model.DeliveryRecord;
import com.agrodairy.management.model.enums.DeliveryShift;
import com.agrodairy.management.model.enums.DeliveryStatus;
import com.agrodairy.management.util.DatabaseUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DeliveryRecordDAOImpl implements DeliveryRecordDAO {

    @Override
    public Long insert(DeliveryRecord rec) {
        String sql = "INSERT INTO delivery_records (customer_id, delivery_date, delivery_shift, delivered_quantity, rate_per_liter, total_amount, status, remarks) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, rec.getCustomerId());
            stmt.setDate(2, rec.getDeliveryDate());
            stmt.setString(3, rec.getDeliveryShift().name());
            stmt.setBigDecimal(4, rec.getDeliveredQuantity());
            stmt.setBigDecimal(5, rec.getRatePerLiter()); // PRICE SNAPSHOT
            stmt.setBigDecimal(6, rec.getTotalAmount());  // delivered_quantity * rate_per_liter
            stmt.setString(7, rec.getStatus().name());
            stmt.setString(8, rec.getRemarks());

            int affected = stmt.executeUpdate();
            if (affected == 0) throw new DatabaseException("Creating delivery record failed, no rows affected.");

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    long id = generatedKeys.getLong(1);
                    rec.setDeliveryId(id);
                    return id;
                } else {
                    throw new DatabaseException("Creating delivery record failed, no ID obtained.");
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to insert delivery record: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean updateStatus(Long deliveryId, DeliveryStatus status, String remarks) {
        String sql = "UPDATE delivery_records SET status = ?, remarks = ? WHERE delivery_id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status.name());
            stmt.setString(2, remarks);
            stmt.setLong(3, deliveryId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update delivery status: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean saveBatchDeliveries(List<DeliveryRecord> deliveryRecords) {
        if (deliveryRecords == null || deliveryRecords.isEmpty()) return true;
        String sql = "INSERT INTO delivery_records (customer_id, delivery_date, delivery_shift, delivered_quantity, rate_per_liter, total_amount, status, remarks) VALUES (?, ?, ?, ?, ?, ?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE delivered_quantity = VALUES(delivered_quantity), rate_per_liter = VALUES(rate_per_liter), total_amount = VALUES(total_amount), status = VALUES(status), remarks = VALUES(remarks)";
        
        Connection conn = null;
        try {
            conn = DatabaseUtil.getConnection();
            conn.setAutoCommit(false);
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                for (DeliveryRecord rec : deliveryRecords) {
                    stmt.setLong(1, rec.getCustomerId());
                    stmt.setDate(2, rec.getDeliveryDate());
                    stmt.setString(3, rec.getDeliveryShift().name());
                    stmt.setBigDecimal(4, rec.getDeliveredQuantity());
                    stmt.setBigDecimal(5, rec.getRatePerLiter());
                    stmt.setBigDecimal(6, rec.getTotalAmount());
                    stmt.setString(7, rec.getStatus().name());
                    stmt.setString(8, rec.getRemarks());
                    stmt.addBatch();
                }
                stmt.executeBatch();
            }
            conn.commit();
            return true;
        } catch (SQLException e) {
            DatabaseUtil.rollbackQuietly(conn);
            throw new DatabaseException("Failed batch insert of delivery records: " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); } catch (Exception ignored) {}
                DatabaseUtil.closeQuietly(conn);
            }
        }
    }

    @Override
    public Optional<DeliveryRecord> findById(Long deliveryId) {
        String sql = "SELECT * FROM delivery_records WHERE delivery_id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, deliveryId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapResultSetToDeliveryRecord(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find delivery record by ID: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<DeliveryRecord> findByCustomerDateShift(Long customerId, Date deliveryDate, DeliveryShift shift) {
        String sql = "SELECT * FROM delivery_records WHERE customer_id = ? AND delivery_date = ? AND delivery_shift = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, customerId);
            stmt.setDate(2, deliveryDate);
            stmt.setString(3, shift.name());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapResultSetToDeliveryRecord(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find delivery record: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public List<DeliveryRecord> findByDateAndShift(Date deliveryDate, DeliveryShift shift) {
        List<DeliveryRecord> records = new ArrayList<>();
        String sql = "SELECT * FROM delivery_records WHERE delivery_date = ? AND delivery_shift = ? ORDER BY delivery_id ASC";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDate(1, deliveryDate);
            stmt.setString(2, shift.name());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    records.add(mapResultSetToDeliveryRecord(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find delivery records by date and shift: " + e.getMessage(), e);
        }
        return records;
    }

    @Override
    public List<DeliveryRecord> findByCustomerAndMonth(Long customerId, int month, int year) {
        List<DeliveryRecord> records = new ArrayList<>();
        String sql = "SELECT * FROM delivery_records WHERE customer_id = ? AND MONTH(delivery_date) = ? AND YEAR(delivery_date) = ? ORDER BY delivery_date ASC, delivery_shift ASC";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, customerId);
            stmt.setInt(2, month);
            stmt.setInt(3, year);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    records.add(mapResultSetToDeliveryRecord(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find delivery records by customer and month: " + e.getMessage(), e);
        }
        return records;
    }

    @Override
    public List<DeliveryRecord> findByDate(Date deliveryDate) {
        List<DeliveryRecord> records = new ArrayList<>();
        String sql = "SELECT * FROM delivery_records WHERE delivery_date = ? ORDER BY delivery_shift ASC, delivery_id ASC";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDate(1, deliveryDate);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    records.add(mapResultSetToDeliveryRecord(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find delivery records by date: " + e.getMessage(), e);
        }
        return records;
    }

    private DeliveryRecord mapResultSetToDeliveryRecord(ResultSet rs) throws SQLException {
        DeliveryRecord r = new DeliveryRecord();
        r.setDeliveryId(rs.getLong("delivery_id"));
        r.setCustomerId(rs.getLong("customer_id"));
        r.setDeliveryDate(rs.getDate("delivery_date"));
        r.setDeliveryShift(DeliveryShift.valueOf(rs.getString("delivery_shift")));
        r.setDeliveredQuantity(rs.getBigDecimal("delivered_quantity"));
        r.setRatePerLiter(rs.getBigDecimal("rate_per_liter"));
        r.setTotalAmount(rs.getBigDecimal("total_amount"));
        r.setStatus(DeliveryStatus.valueOf(rs.getString("status")));
        r.setRemarks(rs.getString("remarks"));
        r.setCreatedAt(rs.getTimestamp("created_at"));
        return r;
    }
}
