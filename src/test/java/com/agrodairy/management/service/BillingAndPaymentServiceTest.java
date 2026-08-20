package com.agrodairy.management.service;

import com.agrodairy.management.dao.MonthlyBillDAO;
import com.agrodairy.management.dao.PaymentDAO;
import com.agrodairy.management.exception.ValidationException;
import com.agrodairy.management.model.MonthlyBill;
import com.agrodairy.management.model.Payment;
import com.agrodairy.management.model.enums.PaymentMode;
import com.agrodairy.management.model.enums.PaymentStatus;
import com.agrodairy.management.service.impl.PaymentServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class BillingAndPaymentServiceTest {

    private PaymentService paymentService;
    private StubMonthlyBillDAO billDAO;
    private StubPaymentDAO paymentDAO;
    private Long testBillId;

    @BeforeEach
    public void setUp() {
        billDAO = new StubMonthlyBillDAO();
        paymentDAO = new StubPaymentDAO();
        paymentService = new PaymentServiceImpl(paymentDAO, billDAO);

        MonthlyBill bill = new MonthlyBill();
        bill.setBillNumber("BILL-202607-101");
        bill.setCustomerId(101L);
        bill.setBillingMonth(7);
        bill.setBillingYear(2026);
        bill.setGrossAmount(new BigDecimal("4800.00"));
        bill.setPreviousBalance(BigDecimal.ZERO);
        bill.setNetPayable(new BigDecimal("4800.00"));
        bill.setPaidAmount(BigDecimal.ZERO);
        bill.setBalanceDue(new BigDecimal("4800.00"));
        bill.setStatus(PaymentStatus.UNPAID);

        testBillId = billDAO.insert(bill);
    }

    @Test
    public void test8_PaymentStatusCalculationAndPartialPayment() {
        Date today = new Date(System.currentTimeMillis());

        // Record Partial Payment of 2000
        Payment p1 = paymentService.recordPayment(testBillId, 101L, new BigDecimal("2000.00"), today, PaymentMode.UPI, "UPI-111");
        assertNotNull(p1.getPaymentId());

        MonthlyBill updatedBill = billDAO.findById(testBillId).get();
        assertEquals(new BigDecimal("2000.00"), updatedBill.getPaidAmount());
        assertEquals(new BigDecimal("2800.00"), updatedBill.getBalanceDue());
        assertEquals(PaymentStatus.PARTIAL, updatedBill.getStatus());

        // Record Remaining Payment of 2800
        Payment p2 = paymentService.recordPayment(testBillId, 101L, new BigDecimal("2800.00"), today, PaymentMode.CASH, "CASH-222");
        assertNotNull(p2.getPaymentId());

        MonthlyBill finalBill = billDAO.findById(testBillId).get();
        assertEquals(new BigDecimal("4800.00"), finalBill.getPaidAmount());
        assertEquals(BigDecimal.ZERO.setScale(2), finalBill.getBalanceDue().setScale(2));
        assertEquals(PaymentStatus.PAID, finalBill.getStatus());
    }

    @Test
    public void test9_NegativePaymentRejection() {
        Date today = new Date(System.currentTimeMillis());

        // Negative Payment Rejection
        assertThrows(ValidationException.class, () -> {
            paymentService.recordPayment(testBillId, 101L, new BigDecimal("-500.00"), today, PaymentMode.CASH, "REF-ERR");
        });

        // Zero Payment Rejection
        assertThrows(ValidationException.class, () -> {
            paymentService.recordPayment(testBillId, 101L, BigDecimal.ZERO, today, PaymentMode.CASH, "REF-ERR");
        });
    }

    private static class StubMonthlyBillDAO implements MonthlyBillDAO {
        private final List<MonthlyBill> bills = new ArrayList<>();
        private long seq = 1;

        @Override public Long insert(MonthlyBill bill) {
            if (bill.getBillId() == null) {
                bill.setBillId(seq++);
            }
            bills.add(bill);
            return bill.getBillId();
        }
        @Override public boolean update(MonthlyBill bill) { return true; }
        @Override public boolean updatePaidAmountAndStatus(Long billId, BigDecimal newPaidAmount, PaymentStatus status) {
            Optional<MonthlyBill> b = findById(billId);
            if (b.isPresent()) {
                MonthlyBill mb = b.get();
                mb.setPaidAmount(newPaidAmount);
                mb.setBalanceDue(mb.getNetPayable().subtract(newPaidAmount));
                mb.setStatus(status);
                return true;
            }
            return false;
        }
        @Override public Optional<MonthlyBill> findById(Long billId) { return bills.stream().filter(b -> b.getBillId().equals(billId)).findFirst(); }
        @Override public Optional<MonthlyBill> findByBillNumber(String num) { return bills.stream().filter(b -> b.getBillNumber().equals(num)).findFirst(); }
        @Override public Optional<MonthlyBill> findByCustomerAndPeriod(Long cId, int m, int y) { return bills.stream().filter(b -> b.getCustomerId().equals(cId) && b.getBillingMonth() == m && b.getBillingYear() == y).findFirst(); }
        @Override public List<MonthlyBill> findByCustomer(Long cId) { return bills; }
        @Override public List<MonthlyBill> findByPeriod(int m, int y) { return bills; }
        @Override public List<MonthlyBill> findPendingBills() { return bills; }
    }

    private static class StubPaymentDAO implements PaymentDAO {
        private final List<Payment> list = new ArrayList<>();
        private long seq = 1;

        @Override public Long insert(Payment p) { p.setPaymentId(seq++); list.add(p); return p.getPaymentId(); }
        @Override public Optional<Payment> findById(Long id) { return list.stream().filter(p -> p.getPaymentId().equals(id)).findFirst(); }
        @Override public List<Payment> findByBillId(Long billId) { return list; }
        @Override public List<Payment> findByCustomerId(Long cId) { return list; }
        @Override public List<Payment> findAll() { return list; }
    }
}
