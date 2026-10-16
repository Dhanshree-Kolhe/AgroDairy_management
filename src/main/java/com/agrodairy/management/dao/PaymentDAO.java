package com.agrodairy.management.dao;

import com.agrodairy.management.model.Payment;
import java.util.List;
import java.util.Optional;

public interface PaymentDAO {
    Long insert(Payment payment);
    default Long insert(Payment payment, java.sql.Connection conn) {
        return insert(payment);
    }
    boolean updateStatus(Long paymentId, String status);
    default boolean updateStatus(Long paymentId, String status, java.sql.Connection conn) {
        return updateStatus(paymentId, status);
    }
    Optional<Payment> findById(Long paymentId);
    List<Payment> findByBillId(Long billId);
    List<Payment> findByCustomerId(Long customerId);
    List<Payment> findAll();
}
