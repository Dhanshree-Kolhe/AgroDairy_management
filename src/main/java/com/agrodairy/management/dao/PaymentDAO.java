package com.agrodairy.management.dao;

import com.agrodairy.management.model.Payment;
import java.util.List;
import java.util.Optional;

public interface PaymentDAO {
    Long insert(Payment payment);
    Optional<Payment> findById(Long paymentId);
    List<Payment> findByBillId(Long billId);
    List<Payment> findByCustomerId(Long customerId);
    List<Payment> findAll();
}
