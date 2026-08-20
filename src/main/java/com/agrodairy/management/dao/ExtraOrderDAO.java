package com.agrodairy.management.dao;

import com.agrodairy.management.model.ExtraOrder;
import com.agrodairy.management.model.enums.RequestStatus;

import java.sql.Date;
import java.util.List;
import java.util.Optional;

public interface ExtraOrderDAO {
    Long insert(ExtraOrder order);
    boolean updateStatus(Long orderId, RequestStatus status);
    Optional<ExtraOrder> findById(Long orderId);
    List<ExtraOrder> findByCustomerId(Long customerId);
    List<ExtraOrder> findByDeliveryDate(Date deliveryDate);
    List<ExtraOrder> findPendingOrders();
    List<ExtraOrder> findByCustomerAndMonth(Long customerId, int month, int year);
}
