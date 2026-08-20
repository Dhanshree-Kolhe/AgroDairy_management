package com.agrodairy.management.dao.impl;

import com.agrodairy.management.dao.HolidayRequestDAO;
import com.agrodairy.management.exception.DatabaseException;
import com.agrodairy.management.model.HolidayRequest;
import com.agrodairy.management.model.enums.RequestStatus;
import com.agrodairy.management.util.DatabaseUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class HolidayRequestDAOImpl implements HolidayRequestDAO {

    @Override
    public Long insert(HolidayRequest request) {
        String sql = "INSERT INTO holiday_requests (customer_id, start_date, end_date, reason, approval_status, vendor_remarks) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, request.getCustomerId());
            stmt.setDate(2, request.getStartDate());
            stmt.setDate(3, request.getEndDate());
            stmt.setString(4, request.getReason());
            stmt.setString(5, request.getApprovalStatus() != null ? request.getApprovalStatus().name() : RequestStatus.PENDING.name());
            stmt.setString(6, request.getVendorRemarks());

            int affected = stmt.executeUpdate();
            if (affected == 0) throw new DatabaseException("Creating holiday request failed, no rows affected.");

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    long id = generatedKeys.getLong(1);
                    request.setRequestId(id);
                    return id;
                } else {
                    throw new DatabaseException("Creating holiday request failed, no ID obtained.");
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to insert holiday request: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean updateStatus(Long requestId, RequestStatus status, String vendorRemarks) {
        String sql = "UPDATE holiday_requests SET approval_status = ?, vendor_remarks = ? WHERE request_id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status.name());
            stmt.setString(2, vendorRemarks);
            stmt.setLong(3, requestId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update holiday request status: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<HolidayRequest> findById(Long requestId) {
        String sql = "SELECT * FROM holiday_requests WHERE request_id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, requestId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapResultSetToHolidayRequest(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find holiday request by ID: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public List<HolidayRequest> findByCustomerId(Long customerId) {
        List<HolidayRequest> requests = new ArrayList<>();
        String sql = "SELECT * FROM holiday_requests WHERE customer_id = ? ORDER BY start_date DESC";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, customerId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    requests.add(mapResultSetToHolidayRequest(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find holiday requests by customer ID: " + e.getMessage(), e);
        }
        return requests;
    }

    @Override
    public List<HolidayRequest> findPendingRequests() {
        List<HolidayRequest> requests = new ArrayList<>();
        String sql = "SELECT * FROM holiday_requests WHERE approval_status = 'PENDING' ORDER BY created_at ASC";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                requests.add(mapResultSetToHolidayRequest(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find pending holiday requests: " + e.getMessage(), e);
        }
        return requests;
    }

    @Override
    public boolean isCustomerOnHoliday(Long customerId, Date date) {
        String sql = "SELECT COUNT(*) FROM holiday_requests WHERE customer_id = ? AND approval_status = 'APPROVED' AND ? BETWEEN start_date AND end_date";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, customerId);
            stmt.setDate(2, date);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to check holiday status: " + e.getMessage(), e);
        }
        return false;
    }

    @Override
    public List<HolidayRequest> findApprovedHolidaysForPeriod(Long customerId, Date startDate, Date endDate) {
        List<HolidayRequest> requests = new ArrayList<>();
        String sql = "SELECT * FROM holiday_requests WHERE customer_id = ? AND approval_status = 'APPROVED' AND (start_date <= ? AND end_date >= ?)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, customerId);
            stmt.setDate(2, endDate);
            stmt.setDate(3, startDate);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    requests.add(mapResultSetToHolidayRequest(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find approved holidays for period: " + e.getMessage(), e);
        }
        return requests;
    }

    private HolidayRequest mapResultSetToHolidayRequest(ResultSet rs) throws SQLException {
        HolidayRequest h = new HolidayRequest();
        h.setRequestId(rs.getLong("request_id"));
        h.setCustomerId(rs.getLong("customer_id"));
        h.setStartDate(rs.getDate("start_date"));
        h.setEndDate(rs.getDate("end_date"));
        h.setReason(rs.getString("reason"));
        h.setApprovalStatus(RequestStatus.valueOf(rs.getString("approval_status")));
        h.setVendorRemarks(rs.getString("vendor_remarks"));
        h.setCreatedAt(rs.getTimestamp("created_at"));
        return h;
    }
}
