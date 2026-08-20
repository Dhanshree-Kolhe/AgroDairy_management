package com.agrodairy.management.dao;

import com.agrodairy.management.model.MonthlyBill;
import com.agrodairy.management.model.enums.PaymentStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface MonthlyBillDAO {
    Long insert(MonthlyBill bill);
    boolean update(MonthlyBill bill);
    boolean updatePaidAmountAndStatus(Long billId, BigDecimal newPaidAmount, PaymentStatus status);
    Optional<MonthlyBill> findById(Long billId);
    Optional<MonthlyBill> findByBillNumber(String billNumber);
    Optional<MonthlyBill> findByCustomerAndPeriod(Long customerId, int month, int year);
    List<MonthlyBill> findByCustomer(Long customerId);
    List<MonthlyBill> findByPeriod(int month, int year);
    List<MonthlyBill> findPendingBills();
}
