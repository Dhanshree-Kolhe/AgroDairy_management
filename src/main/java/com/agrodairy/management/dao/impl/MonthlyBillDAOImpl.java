package com.agrodairy.management.dao.impl;

import com.agrodairy.management.dao.MonthlyBillDAO;
import com.agrodairy.management.exception.DatabaseException;
import com.agrodairy.management.model.MonthlyBill;
import com.agrodairy.management.model.enums.PaymentStatus;
import com.agrodairy.management.util.DatabaseUtil;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MonthlyBillDAOImpl implements MonthlyBillDAO {

    @Override
    public Long insert(MonthlyBill bill) {
        String sql = "INSERT INTO monthly_bills (bill_number, customer_id, billing_month, billing_year, total_milk_quantity, milk_charges, extra_orders_charges, holiday_deductions, gross_amount, previous_balance, net_payable, paid_amount, balance_due, status, bill_date, due_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, bill.getBillNumber());
            stmt.setLong(2, bill.getCustomerId());
            stmt.setInt(3, bill.getBillingMonth());
            stmt.setInt(4, bill.getBillingYear());
            stmt.setBigDecimal(5, bill.getTotalMilkQuantity());
            stmt.setBigDecimal(6, bill.getMilkCharges());
            stmt.setBigDecimal(7, bill.getExtraOrdersCharges());
            stmt.setBigDecimal(8, bill.getHolidayDeductions());
            stmt.setBigDecimal(9, bill.getGrossAmount());
            stmt.setBigDecimal(10, bill.getPreviousBalance());
            stmt.setBigDecimal(11, bill.getNetPayable());
            stmt.setBigDecimal(12, bill.getPaidAmount());
            stmt.setBigDecimal(13, bill.getBalanceDue());
            stmt.setString(14, bill.getStatus().name());
            stmt.setDate(15, bill.getBillDate());
            stmt.setDate(16, bill.getDueDate());

            int affected = stmt.executeUpdate();
            if (affected == 0) throw new DatabaseException("Creating monthly bill failed, no rows affected.");

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    long id = generatedKeys.getLong(1);
                    bill.setBillId(id);
                    return id;
                } else {
                    throw new DatabaseException("Creating monthly bill failed, no ID obtained.");
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to insert monthly bill: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean update(MonthlyBill bill) {
        String sql = "UPDATE monthly_bills SET total_milk_quantity = ?, milk_charges = ?, extra_orders_charges = ?, holiday_deductions = ?, gross_amount = ?, previous_balance = ?, net_payable = ?, paid_amount = ?, balance_due = ?, status = ?, due_date = ? WHERE bill_id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBigDecimal(1, bill.getTotalMilkQuantity());
            stmt.setBigDecimal(2, bill.getMilkCharges());
            stmt.setBigDecimal(3, bill.getExtraOrdersCharges());
            stmt.setBigDecimal(4, bill.getHolidayDeductions());
            stmt.setBigDecimal(5, bill.getGrossAmount());
            stmt.setBigDecimal(6, bill.getPreviousBalance());
            stmt.setBigDecimal(7, bill.getNetPayable());
            stmt.setBigDecimal(8, bill.getPaidAmount());
            stmt.setBigDecimal(9, bill.getBalanceDue());
            stmt.setString(10, bill.getStatus().name());
            stmt.setDate(11, bill.getDueDate());
            stmt.setLong(12, bill.getBillId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update monthly bill: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean updatePaidAmountAndStatus(Long billId, BigDecimal newPaidAmount, PaymentStatus status) {
        try (Connection conn = DatabaseUtil.getConnection()) {
            return updatePaidAmountAndStatus(billId, newPaidAmount, status, conn);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to acquire connection for bill payment update: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean updatePaidAmountAndStatus(Long billId, BigDecimal newPaidAmount, PaymentStatus status, Connection conn) {
        String sql = "UPDATE monthly_bills SET paid_amount = ?, balance_due = (net_payable - ?), status = ? WHERE bill_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBigDecimal(1, newPaidAmount);
            stmt.setBigDecimal(2, newPaidAmount);
            stmt.setString(3, status.name());
            stmt.setLong(4, billId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update bill payment status: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<MonthlyBill> findById(Long billId) {
        String sql = "SELECT * FROM monthly_bills WHERE bill_id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, billId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapResultSetToBill(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find bill by ID: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<MonthlyBill> findByBillNumber(String billNumber) {
        String sql = "SELECT * FROM monthly_bills WHERE bill_number = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, billNumber);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapResultSetToBill(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find bill by bill number: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<MonthlyBill> findByCustomerAndPeriod(Long customerId, int month, int year) {
        String sql = "SELECT * FROM monthly_bills WHERE customer_id = ? AND billing_month = ? AND billing_year = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, customerId);
            stmt.setInt(2, month);
            stmt.setInt(3, year);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapResultSetToBill(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find bill by customer and period: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public List<MonthlyBill> findByCustomer(Long customerId) {
        List<MonthlyBill> bills = new ArrayList<>();
        String sql = "SELECT * FROM monthly_bills WHERE customer_id = ? ORDER BY billing_year DESC, billing_month DESC";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, customerId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    bills.add(mapResultSetToBill(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find bills by customer: " + e.getMessage(), e);
        }
        return bills;
    }

    @Override
    public List<MonthlyBill> findByPeriod(int month, int year) {
        List<MonthlyBill> bills = new ArrayList<>();
        String sql = "SELECT * FROM monthly_bills WHERE billing_month = ? AND billing_year = ? ORDER BY bill_id ASC";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, month);
            stmt.setInt(2, year);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    bills.add(mapResultSetToBill(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find bills by period: " + e.getMessage(), e);
        }
        return bills;
    }

    @Override
    public List<MonthlyBill> findPendingBills() {
        List<MonthlyBill> bills = new ArrayList<>();
        String sql = "SELECT * FROM monthly_bills WHERE status != 'PAID' ORDER BY due_date ASC";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                bills.add(mapResultSetToBill(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find pending bills: " + e.getMessage(), e);
        }
        return bills;
    }

    private MonthlyBill mapResultSetToBill(ResultSet rs) throws SQLException {
        MonthlyBill b = new MonthlyBill();
        b.setBillId(rs.getLong("bill_id"));
        b.setBillNumber(rs.getString("bill_number"));
        b.setCustomerId(rs.getLong("customer_id"));
        b.setBillingMonth(rs.getInt("billing_month"));
        b.setBillingYear(rs.getInt("billing_year"));
        b.setTotalMilkQuantity(rs.getBigDecimal("total_milk_quantity"));
        b.setMilkCharges(rs.getBigDecimal("milk_charges"));
        b.setExtraOrdersCharges(rs.getBigDecimal("extra_orders_charges"));
        b.setHolidayDeductions(rs.getBigDecimal("holiday_deductions"));
        b.setGrossAmount(rs.getBigDecimal("gross_amount"));
        b.setPreviousBalance(rs.getBigDecimal("previous_balance"));
        b.setNetPayable(rs.getBigDecimal("net_payable"));
        b.setPaidAmount(rs.getBigDecimal("paid_amount"));
        b.setBalanceDue(rs.getBigDecimal("balance_due"));
        b.setStatus(PaymentStatus.valueOf(rs.getString("status")));
        b.setBillDate(rs.getDate("bill_date"));
        b.setDueDate(rs.getDate("due_date"));
        b.setCreatedAt(rs.getTimestamp("created_at"));
        return b;
    }
}
