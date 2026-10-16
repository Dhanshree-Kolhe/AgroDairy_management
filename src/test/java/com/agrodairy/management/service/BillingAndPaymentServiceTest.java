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

    @Test
    public void test10_BankTransferPendingVerificationAndVendorApproval() {
        Date today = new Date(System.currentTimeMillis());

        // 1. Submit Bank Transfer for verification
        Payment pending = paymentService.recordPendingPayment(testBillId, 101L, new BigDecimal("1500.00"), today, PaymentMode.NET_BANKING, "UTR-123456789");
        assertNotNull(pending.getPaymentId());
        assertEquals("PENDING", pending.getStatus());

        // Ledger must NOT be credited before verification
        MonthlyBill billBefore = billDAO.findById(testBillId).get();
        assertEquals(BigDecimal.ZERO, billBefore.getPaidAmount());
        assertEquals(new BigDecimal("4800.00"), billBefore.getBalanceDue());

        // Prevent duplicate pending payment for the same bill
        assertThrows(ValidationException.class, () -> {
            paymentService.recordPendingPayment(testBillId, 101L, new BigDecimal("500.00"), today, PaymentMode.NET_BANKING, "UTR-99999");
        });

        // 2. Vendor / Admin verifies payment
        boolean verified = paymentService.verifyPayment(pending.getPaymentId());
        assertTrue(verified);

        Payment verifiedPayment = paymentDAO.findById(pending.getPaymentId()).get();
        assertEquals("SUCCESS", verifiedPayment.getStatus());

        // Bill ledger is now credited
        MonthlyBill billAfter = billDAO.findById(testBillId).get();
        assertEquals(new BigDecimal("1500.00"), billAfter.getPaidAmount());
        assertEquals(new BigDecimal("3300.00"), billAfter.getBalanceDue());
        assertEquals(PaymentStatus.PARTIAL, billAfter.getStatus());
    }

    @Test
    public void test11_PreventDuplicatePaymentOnSettledBillAndOverpayment() {
        Date today = new Date(System.currentTimeMillis());

        // Settle bill in full
        paymentService.recordPayment(testBillId, 101L, new BigDecimal("4800.00"), today, PaymentMode.CASH, "CASH-FULL");

        MonthlyBill settledBill = billDAO.findById(testBillId).get();
        assertEquals(PaymentStatus.PAID, settledBill.getStatus());

        // Reject duplicate payment on settled bill
        assertThrows(ValidationException.class, () -> {
            paymentService.recordPayment(testBillId, 101L, new BigDecimal("100.00"), today, PaymentMode.CASH, "CASH-EXTRA");
        });
    }

    @Test
    public void test12_ZeroBalanceBillStatusAndSettlement() {
        Date today = new Date(System.currentTimeMillis());

        // Create zero-balance bill (e.g. month with no orders or charges)
        MonthlyBill zeroBill = new MonthlyBill();
        zeroBill.setBillNumber("BILL-202602-1");
        zeroBill.setCustomerId(101L);
        zeroBill.setBillingMonth(2);
        zeroBill.setBillingYear(2026);
        zeroBill.setGrossAmount(BigDecimal.ZERO);
        zeroBill.setPreviousBalance(BigDecimal.ZERO);
        zeroBill.setNetPayable(BigDecimal.ZERO);
        zeroBill.setPaidAmount(BigDecimal.ZERO);
        zeroBill.setBalanceDue(BigDecimal.ZERO);
        zeroBill.setStatus(PaymentStatus.UNPAID); // As previously stored in DB

        Long zeroBillId = billDAO.insert(zeroBill);

        // Reconcile bill state using BillingServiceImpl
        com.agrodairy.management.service.impl.BillingServiceImpl billingServiceImpl =
                new com.agrodairy.management.service.impl.BillingServiceImpl(billDAO, null, null, null, paymentDAO);

        billingServiceImpl.reconcileBillState(zeroBill);

        // Status must be resolved to PAID and balance to 0.00
        assertEquals(PaymentStatus.PAID, zeroBill.getStatus());
        assertEquals(BigDecimal.ZERO, zeroBill.getBalanceDue());

        // Duplicate payment on zero-balance bill must be rejected
        assertThrows(ValidationException.class, () -> {
            paymentService.recordPayment(zeroBillId, 101L, new BigDecimal("100.00"), today, PaymentMode.UPI, "UPI-ZERO");
        });
    }

    @Test
    public void test13_PendingVerificationBlocksDuplicatePayments() {
        Date today = new Date(System.currentTimeMillis());

        // Submit Bank Transfer (status PENDING)
        Payment pending = paymentService.recordPendingPayment(testBillId, 101L, new BigDecimal("1000.00"), today, PaymentMode.NET_BANKING, "UTR-PENDING-1");
        assertNotNull(pending);
        assertEquals("PENDING", pending.getStatus());

        // Duplicate payment must be blocked while verification is pending
        assertThrows(ValidationException.class, () -> {
            paymentService.recordPayment(testBillId, 101L, new BigDecimal("1000.00"), today, PaymentMode.UPI, "UPI-DUP");
        });

        // Duplicate pending request must also be blocked
        assertThrows(ValidationException.class, () -> {
            paymentService.recordPendingPayment(testBillId, 101L, new BigDecimal("500.00"), today, PaymentMode.NET_BANKING, "UTR-PENDING-2");
        });
    }

    @Test
    public void test14_CashOnDeliveryPendingCollectionWorkflow() {
        Date today = new Date(System.currentTimeMillis());

        // Customer requests COD doorstep collection
        Payment codPayment = paymentService.recordPendingPayment(testBillId, 101L, new BigDecimal("2400.00"), today, PaymentMode.CASH, null);
        assertNotNull(codPayment.getPaymentId());
        assertEquals("PENDING", codPayment.getStatus());
        assertTrue(codPayment.getTransactionReference().startsWith("COD-"));

        // Bill ledger must remain uncredited before vendor confirms collection
        MonthlyBill billBefore = billDAO.findById(testBillId).get();
        assertEquals(BigDecimal.ZERO, billBefore.getPaidAmount());
        assertEquals(new BigDecimal("4800.00"), billBefore.getBalanceDue());

        // Delivery agent/vendor confirms cash collection
        boolean verified = paymentService.verifyPayment(codPayment.getPaymentId());
        assertTrue(verified);

        // Ledger is now credited
        MonthlyBill billAfter = billDAO.findById(testBillId).get();
        assertEquals(new BigDecimal("2400.00"), billAfter.getPaidAmount());
        assertEquals(new BigDecimal("2400.00"), billAfter.getBalanceDue());
        assertEquals(PaymentStatus.PARTIAL, billAfter.getStatus());
    }

    @Test
    public void test15_BillStateReconciliationTruth() {
        com.agrodairy.management.service.impl.BillingServiceImpl billingServiceImpl =
                new com.agrodairy.management.service.impl.BillingServiceImpl(billDAO, null, null, null, paymentDAO);

        // 1. Unpaid Bill
        MonthlyBill b1 = new MonthlyBill();
        b1.setNetPayable(new BigDecimal("3000.00"));
        b1.setPaidAmount(BigDecimal.ZERO);
        b1.setBalanceDue(new BigDecimal("3000.00"));
        b1.setStatus(PaymentStatus.UNPAID);
        billingServiceImpl.reconcileBillState(b1);
        assertEquals(PaymentStatus.UNPAID, b1.getStatus());
        assertEquals(new BigDecimal("3000.00"), b1.getBalanceDue());

        // 2. Partially Paid Bill
        MonthlyBill b2 = new MonthlyBill();
        b2.setNetPayable(new BigDecimal("3000.00"));
        b2.setPaidAmount(new BigDecimal("1000.00"));
        b2.setBalanceDue(new BigDecimal("2000.00"));
        b2.setStatus(PaymentStatus.UNPAID); // Out of sync DB status
        billingServiceImpl.reconcileBillState(b2);
        assertEquals(PaymentStatus.PARTIAL, b2.getStatus());
        assertEquals(new BigDecimal("2000.00"), b2.getBalanceDue());

        // 3. Fully Paid Bill
        MonthlyBill b3 = new MonthlyBill();
        b3.setNetPayable(new BigDecimal("3000.00"));
        b3.setPaidAmount(new BigDecimal("3000.00"));
        b3.setBalanceDue(BigDecimal.ZERO);
        b3.setStatus(PaymentStatus.PARTIAL); // Out of sync DB status
        billingServiceImpl.reconcileBillState(b3);
        assertEquals(PaymentStatus.PAID, b3.getStatus());
        assertEquals(BigDecimal.ZERO, b3.getBalanceDue());
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
        @Override public boolean updateStatus(Long paymentId, String status) {
            Optional<Payment> p = findById(paymentId);
            if (p.isPresent()) {
                p.get().setStatus(status);
                return true;
            }
            return false;
        }
        @Override public Optional<Payment> findById(Long id) { return list.stream().filter(p -> p.getPaymentId().equals(id)).findFirst(); }
        @Override public List<Payment> findByBillId(Long billId) { return list; }
        @Override public List<Payment> findByCustomerId(Long cId) { return list; }
        @Override public List<Payment> findAll() { return list; }
    }
}
