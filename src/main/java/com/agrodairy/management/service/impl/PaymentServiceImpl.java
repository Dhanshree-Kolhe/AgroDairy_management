package com.agrodairy.management.service.impl;

import com.agrodairy.management.dao.MonthlyBillDAO;
import com.agrodairy.management.dao.PaymentDAO;
import com.agrodairy.management.dao.impl.MonthlyBillDAOImpl;
import com.agrodairy.management.dao.impl.PaymentDAOImpl;
import com.agrodairy.management.exception.DatabaseException;
import com.agrodairy.management.exception.ResourceNotFoundException;
import com.agrodairy.management.exception.ValidationException;
import com.agrodairy.management.model.MonthlyBill;
import com.agrodairy.management.model.Payment;
import com.agrodairy.management.model.enums.PaymentMode;
import com.agrodairy.management.model.enums.PaymentStatus;
import com.agrodairy.management.service.PaymentService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.util.List;

public class PaymentServiceImpl implements PaymentService {
    private final PaymentDAO paymentDAO;
    private final MonthlyBillDAO monthlyBillDAO;

    public PaymentServiceImpl() {
        this.paymentDAO = new PaymentDAOImpl();
        this.monthlyBillDAO = new MonthlyBillDAOImpl();
    }

    public PaymentServiceImpl(PaymentDAO paymentDAO, MonthlyBillDAO monthlyBillDAO) {
        this.paymentDAO = paymentDAO;
        this.monthlyBillDAO = monthlyBillDAO;
    }

    @Override
    public Payment recordPayment(Long billId, Long customerId, BigDecimal amountPaid, Date paymentDate, PaymentMode mode, String reference) {
        if (billId == null || billId <= 0) throw new ValidationException("Invalid bill ID.");
        if (customerId == null || customerId <= 0) throw new ValidationException("Invalid customer ID.");
        if (amountPaid == null || amountPaid.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Payment amount must be greater than zero. Negative or zero payments are rejected.");
        }
        if (mode == null) throw new ValidationException("Payment mode is required.");
        if (paymentDate == null) paymentDate = new Date(System.currentTimeMillis());

        MonthlyBill bill = monthlyBillDAO.findById(billId)
                .orElseThrow(() -> new ResourceNotFoundException("Monthly bill not found with ID: " + billId));

        if (!bill.getCustomerId().equals(customerId)) {
            throw new ValidationException("Bill does not belong to customer ID: " + customerId);
        }

        BigDecimal newPaidTotal = bill.getPaidAmount().add(amountPaid).setScale(2, RoundingMode.HALF_UP);
        BigDecimal newBalanceDue = bill.getNetPayable().subtract(newPaidTotal).setScale(2, RoundingMode.HALF_UP);

        PaymentStatus newStatus = PaymentStatus.PARTIAL;
        if (newBalanceDue.compareTo(BigDecimal.ZERO) <= 0) {
            newStatus = PaymentStatus.PAID;
            newBalanceDue = BigDecimal.ZERO;
        }

        // Record payment & update bill status
        Payment payment = new Payment();
        payment.setBillId(billId);
        payment.setCustomerId(customerId);
        payment.setAmountPaid(amountPaid);
        payment.setPaymentDate(paymentDate);
        payment.setPaymentMode(mode);
        payment.setTransactionReference(reference);
        payment.setStatus("SUCCESS");

        Long id = paymentDAO.insert(payment);
        payment.setPaymentId(id);

        monthlyBillDAO.updatePaidAmountAndStatus(billId, newPaidTotal, newStatus);
        return payment;
    }

    @Override
    public Payment getPaymentById(Long paymentId) {
        if (paymentId == null || paymentId <= 0) throw new ValidationException("Invalid payment ID.");
        return paymentDAO.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with ID: " + paymentId));
    }

    @Override
    public List<Payment> getPaymentsByBill(Long billId) {
        if (billId == null || billId <= 0) throw new ValidationException("Invalid bill ID.");
        return paymentDAO.findByBillId(billId);
    }

    @Override
    public List<Payment> getPaymentsByCustomer(Long customerId) {
        if (customerId == null || customerId <= 0) throw new ValidationException("Invalid customer ID.");
        return paymentDAO.findByCustomerId(customerId);
    }

    @Override
    public List<Payment> getAllPayments() {
        return paymentDAO.findAll();
    }
}
