package com.agrodairy.management.dao.impl;

import com.agrodairy.management.dao.PaymentDAO;
import com.agrodairy.management.exception.DatabaseException;
import com.agrodairy.management.model.Payment;
import com.agrodairy.management.model.enums.PaymentMode;
import com.agrodairy.management.util.DatabaseUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PaymentDAOImpl implements PaymentDAO {

    @Override
    public Long insert(Payment payment) {
        String sql = "INSERT INTO payments (bill_id, customer_id, amount_paid, payment_date, payment_mode, transaction_reference, status) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, payment.getBillId());
            stmt.setLong(2, payment.getCustomerId());
            stmt.setBigDecimal(3, payment.getAmountPaid());
            stmt.setDate(4, payment.getPaymentDate());
            stmt.setString(5, payment.getPaymentMode().name());
            stmt.setString(6, payment.getTransactionReference());
            stmt.setString(7, payment.getStatus() != null ? payment.getStatus() : "SUCCESS");

            int affected = stmt.executeUpdate();
            if (affected == 0) throw new DatabaseException("Creating payment record failed, no rows affected.");

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    long id = generatedKeys.getLong(1);
                    payment.setPaymentId(id);
                    return id;
                } else {
                    throw new DatabaseException("Creating payment record failed, no ID obtained.");
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to insert payment record: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Payment> findById(Long paymentId) {
        String sql = "SELECT * FROM payments WHERE payment_id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, paymentId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapResultSetToPayment(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find payment by ID: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public List<Payment> findByBillId(Long billId) {
        List<Payment> payments = new ArrayList<>();
        String sql = "SELECT * FROM payments WHERE bill_id = ? ORDER BY payment_date DESC";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, billId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    payments.add(mapResultSetToPayment(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find payments by bill ID: " + e.getMessage(), e);
        }
        return payments;
    }

    @Override
    public List<Payment> findByCustomerId(Long customerId) {
        List<Payment> payments = new ArrayList<>();
        String sql = "SELECT * FROM payments WHERE customer_id = ? ORDER BY payment_date DESC";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, customerId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    payments.add(mapResultSetToPayment(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find payments by customer ID: " + e.getMessage(), e);
        }
        return payments;
    }

    @Override
    public List<Payment> findAll() {
        List<Payment> payments = new ArrayList<>();
        String sql = "SELECT * FROM payments ORDER BY payment_date DESC";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                payments.add(mapResultSetToPayment(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to retrieve all payments: " + e.getMessage(), e);
        }
        return payments;
    }

    private Payment mapResultSetToPayment(ResultSet rs) throws SQLException {
        Payment p = new Payment();
        p.setPaymentId(rs.getLong("payment_id"));
        p.setBillId(rs.getLong("bill_id"));
        p.setCustomerId(rs.getLong("customer_id"));
        p.setAmountPaid(rs.getBigDecimal("amount_paid"));
        p.setPaymentDate(rs.getDate("payment_date"));
        p.setPaymentMode(PaymentMode.valueOf(rs.getString("payment_mode")));
        p.setTransactionReference(rs.getString("transaction_reference"));
        p.setStatus(rs.getString("status"));
        p.setCreatedAt(rs.getTimestamp("created_at"));
        return p;
    }
}
