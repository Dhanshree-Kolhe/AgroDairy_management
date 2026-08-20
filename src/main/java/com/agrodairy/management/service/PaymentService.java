package com.agrodairy.management.service;

import com.agrodairy.management.model.Payment;
import com.agrodairy.management.model.enums.PaymentMode;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.List;

public interface PaymentService {
    Payment recordPayment(Long billId, Long customerId, BigDecimal amountPaid, Date paymentDate, PaymentMode mode, String reference);
    Payment getPaymentById(Long paymentId);
    List<Payment> getPaymentsByBill(Long billId);
    List<Payment> getPaymentsByCustomer(Long customerId);
    List<Payment> getAllPayments();
}
