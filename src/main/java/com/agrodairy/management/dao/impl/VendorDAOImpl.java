package com.agrodairy.management.dao.impl;

import com.agrodairy.management.dao.VendorDAO;
import com.agrodairy.management.exception.DatabaseException;
import com.agrodairy.management.model.Vendor;
import com.agrodairy.management.util.DatabaseUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class VendorDAOImpl implements VendorDAO {

    @Override
    public Long insert(Vendor vendor) {
        String sql = "INSERT INTO vendors (user_id, vendor_code, business_name, owner_name, phone, email, address, gst_number) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, vendor.getUserId());
            stmt.setString(2, vendor.getVendorCode());
            stmt.setString(3, vendor.getBusinessName());
            stmt.setString(4, vendor.getOwnerName());
            stmt.setString(5, vendor.getPhone());
            stmt.setString(6, vendor.getEmail());
            stmt.setString(7, vendor.getAddress());
            stmt.setString(8, vendor.getGstNumber());

            int affected = stmt.executeUpdate();
            if (affected == 0) throw new DatabaseException("Creating vendor failed, no rows affected.");

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    long id = generatedKeys.getLong(1);
                    vendor.setVendorId(id);
                    return id;
                } else {
                    throw new DatabaseException("Creating vendor failed, no ID obtained.");
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to insert vendor: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean update(Vendor vendor) {
        String sql = "UPDATE vendors SET business_name = ?, owner_name = ?, phone = ?, email = ?, address = ?, gst_number = ? WHERE vendor_id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, vendor.getBusinessName());
            stmt.setString(2, vendor.getOwnerName());
            stmt.setString(3, vendor.getPhone());
            stmt.setString(4, vendor.getEmail());
            stmt.setString(5, vendor.getAddress());
            stmt.setString(6, vendor.getGstNumber());
            stmt.setLong(7, vendor.getVendorId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update vendor: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean delete(Long vendorId) {
        String sql = "DELETE FROM vendors WHERE vendor_id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, vendorId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete vendor: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Vendor> findById(Long vendorId) {
        String sql = "SELECT * FROM vendors WHERE vendor_id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, vendorId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapResultSetToVendor(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find vendor by ID: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Vendor> findByUserId(Long userId) {
        String sql = "SELECT * FROM vendors WHERE user_id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapResultSetToVendor(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find vendor by user ID: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Vendor> findByVendorCode(String vendorCode) {
        String sql = "SELECT * FROM vendors WHERE vendor_code = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, vendorCode);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapResultSetToVendor(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find vendor by vendor code: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public List<Vendor> findAll() {
        List<Vendor> vendors = new ArrayList<>();
        String sql = "SELECT * FROM vendors ORDER BY vendor_id ASC";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                vendors.add(mapResultSetToVendor(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to retrieve vendors: " + e.getMessage(), e);
        }
        return vendors;
    }

    private Vendor mapResultSetToVendor(ResultSet rs) throws SQLException {
        Vendor v = new Vendor();
        v.setVendorId(rs.getLong("vendor_id"));
        v.setUserId(rs.getLong("user_id"));
        v.setVendorCode(rs.getString("vendor_code"));
        v.setBusinessName(rs.getString("business_name"));
        v.setOwnerName(rs.getString("owner_name"));
        v.setPhone(rs.getString("phone"));
        v.setEmail(rs.getString("email"));
        v.setAddress(rs.getString("address"));
        v.setGstNumber(rs.getString("gst_number"));
        v.setCreatedAt(rs.getTimestamp("created_at"));
        return v;
    }
}
