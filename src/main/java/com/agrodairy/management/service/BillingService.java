package com.agrodairy.management.service;

import com.agrodairy.management.model.MonthlyBill;

import java.math.BigDecimal;
import java.util.List;

public interface BillingService {
    MonthlyBill generateMonthlyBill(Long customerId, int month, int year);
    MonthlyBill getCustomerMonthlyBill(Long customerId, int month, int year);
    List<MonthlyBill> getCustomerBillHistory(Long customerId);
    List<MonthlyBill> getMonthlyBillsByPeriod(int month, int year);
    List<MonthlyBill> getPendingBills();
    BigDecimal getOutstandingBalance(Long customerId);
    MonthlyBill recalculateMonthlyBill(Long billId);
}
