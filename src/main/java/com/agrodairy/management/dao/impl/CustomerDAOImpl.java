package com.agrodairy.management.dao.impl;

import com.agrodairy.management.dao.CustomerDAO;
import com.agrodairy.management.exception.DatabaseException;
import com.agrodairy.management.model.Customer;
import com.agrodairy.management.model.enums.DeliveryShift;
import com.agrodairy.management.model.enums.MilkType;
import com.agrodairy.management.model.enums.SubscriptionStatus;
import com.agrodairy.management.util.DatabaseUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CustomerDAOImpl implements CustomerDAO {

    @Override
    public Long insert(Customer c) {
        String sql = "INSERT INTO customers (user_id, vendor_id, customer_code, full_name, phone, house_number, street, area, city, pincode, address, latitude, longitude, milk_type, morning_quantity, evening_quantity, delivery_shift, current_price_per_liter, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (c.getUserId() != null) stmt.setLong(1, c.getUserId()); else stmt.setNull(1, Types.BIGINT);
            stmt.setLong(2, c.getVendorId());
            stmt.setString(3, c.getCustomerCode());
            stmt.setString(4, c.getFullName());
            stmt.setString(5, c.getPhone());
            stmt.setString(6, c.getHouseNumber());
            stmt.setString(7, c.getStreet());
            stmt.setString(8, c.getArea());
            stmt.setString(9, c.getCity() != null ? c.getCity() : "Nagpur");
            stmt.setString(10, c.getPincode());
            stmt.setString(11, c.getAddress());
            if (c.getLatitude() != null) stmt.setBigDecimal(12, c.getLatitude()); else stmt.setNull(12, Types.DECIMAL);
            if (c.getLongitude() != null) stmt.setBigDecimal(13, c.getLongitude()); else stmt.setNull(13, Types.DECIMAL);
            stmt.setString(14, c.getMilkType().name());
            stmt.setBigDecimal(15, c.getMorningQuantity());
            stmt.setBigDecimal(16, c.getEveningQuantity());
            stmt.setString(17, c.getDeliveryShift().name());
            stmt.setBigDecimal(18, c.getCurrentPricePerLiter());
            stmt.setString(19, c.getStatus() != null ? c.getStatus().name() : SubscriptionStatus.ACTIVE.name());

            int affected = stmt.executeUpdate();
            if (affected == 0) throw new DatabaseException("Creating customer failed, no rows affected.");

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    long id = generatedKeys.getLong(1);
                    c.setCustomerId(id);
                    return id;
                } else {
                    throw new DatabaseException("Creating customer failed, no ID obtained.");
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to insert customer: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean update(Customer c) {
        String sql = "UPDATE customers SET full_name = ?, phone = ?, house_number = ?, street = ?, area = ?, city = ?, pincode = ?, address = ?, latitude = ?, longitude = ?, milk_type = ?, morning_quantity = ?, evening_quantity = ?, delivery_shift = ?, current_price_per_liter = ?, status = ? WHERE customer_id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, c.getFullName());
            stmt.setString(2, c.getPhone());
            stmt.setString(3, c.getHouseNumber());
            stmt.setString(4, c.getStreet());
            stmt.setString(5, c.getArea());
            stmt.setString(6, c.getCity());
            stmt.setString(7, c.getPincode());
            stmt.setString(8, c.getAddress());
            if (c.getLatitude() != null) stmt.setBigDecimal(9, c.getLatitude()); else stmt.setNull(9, Types.DECIMAL);
            if (c.getLongitude() != null) stmt.setBigDecimal(10, c.getLongitude()); else stmt.setNull(10, Types.DECIMAL);
            stmt.setString(11, c.getMilkType().name());
            stmt.setBigDecimal(12, c.getMorningQuantity());
            stmt.setBigDecimal(13, c.getEveningQuantity());
            stmt.setString(14, c.getDeliveryShift().name());
            stmt.setBigDecimal(15, c.getCurrentPricePerLiter());
            stmt.setString(16, c.getStatus().name());
            stmt.setLong(17, c.getCustomerId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update customer: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean updateStatus(Long customerId, SubscriptionStatus status) {
        String sql = "UPDATE customers SET status = ? WHERE customer_id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status.name());
            stmt.setLong(2, customerId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update customer status: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean softDelete(Long customerId) {
        return updateStatus(customerId, SubscriptionStatus.INACTIVE);
    }

    @Override
    public Optional<Customer> findById(Long customerId) {
        String sql = "SELECT * FROM customers WHERE customer_id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, customerId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapResultSetToCustomer(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find customer by ID: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Customer> findByCustomerCode(String customerCode) {
        String sql = "SELECT * FROM customers WHERE customer_code = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, customerCode);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapResultSetToCustomer(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find customer by customer code: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Customer> findByUserId(Long userId) {
        String sql = "SELECT * FROM customers WHERE user_id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapResultSetToCustomer(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find customer by user ID: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public List<Customer> findAll() {
        List<Customer> customers = new ArrayList<>();
        String sql = "SELECT * FROM customers ORDER BY customer_id ASC";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                customers.add(mapResultSetToCustomer(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to retrieve all customers: " + e.getMessage(), e);
        }
        return customers;
    }

    @Override
    public List<Customer> findByVendorId(Long vendorId) {
        List<Customer> customers = new ArrayList<>();
        String sql = "SELECT * FROM customers WHERE vendor_id = ? ORDER BY full_name ASC";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, vendorId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    customers.add(mapResultSetToCustomer(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to retrieve customers by vendor ID: " + e.getMessage(), e);
        }
        return customers;
    }

    @Override
    public List<Customer> findByArea(String area) {
        List<Customer> customers = new ArrayList<>();
        String sql = "SELECT * FROM customers WHERE area = ? ORDER BY full_name ASC";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, area);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    customers.add(mapResultSetToCustomer(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find customers by area: " + e.getMessage(), e);
        }
        return customers;
    }

    @Override
    public List<Customer> search(String query) {
        List<Customer> customers = new ArrayList<>();
        String pattern = "%" + (query != null ? query.trim() : "") + "%";
        String sql = "SELECT * FROM customers WHERE full_name LIKE ? OR phone LIKE ? OR customer_code LIKE ? OR area LIKE ? ORDER BY full_name ASC";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, pattern);
            stmt.setString(2, pattern);
            stmt.setString(3, pattern);
            stmt.setString(4, pattern);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    customers.add(mapResultSetToCustomer(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to search customers: " + e.getMessage(), e);
        }
        return customers;
    }

    private Customer mapResultSetToCustomer(ResultSet rs) throws SQLException {
        Customer c = new Customer();
        c.setCustomerId(rs.getLong("customer_id"));
        long uid = rs.getLong("user_id");
        if (!rs.wasNull()) c.setUserId(uid);
        c.setVendorId(rs.getLong("vendor_id"));
        c.setCustomerCode(rs.getString("customer_code"));
        c.setFullName(rs.getString("full_name"));
        c.setPhone(rs.getString("phone"));
        c.setHouseNumber(rs.getString("house_number"));
        c.setStreet(rs.getString("street"));
        c.setArea(rs.getString("area"));
        c.setCity(rs.getString("city"));
        c.setPincode(rs.getString("pincode"));
        c.setAddress(rs.getString("address"));
        c.setLatitude(rs.getBigDecimal("latitude"));
        c.setLongitude(rs.getBigDecimal("longitude"));
        c.setMilkType(MilkType.valueOf(rs.getString("milk_type")));
        c.setMorningQuantity(rs.getBigDecimal("morning_quantity"));
        c.setEveningQuantity(rs.getBigDecimal("evening_quantity"));
        c.setDeliveryShift(DeliveryShift.valueOf(rs.getString("delivery_shift")));
        c.setCurrentPricePerLiter(rs.getBigDecimal("current_price_per_liter"));
        c.setStatus(SubscriptionStatus.valueOf(rs.getString("status")));
        c.setCreatedAt(rs.getTimestamp("created_at"));
        c.setUpdatedAt(rs.getTimestamp("updated_at"));
        return c;
    }
}
