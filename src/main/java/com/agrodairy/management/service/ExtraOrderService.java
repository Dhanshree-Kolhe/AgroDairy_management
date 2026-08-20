package com.agrodairy.management.service;

import com.agrodairy.management.model.ExtraOrder;
import com.agrodairy.management.model.enums.RequestStatus;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.List;

public interface ExtraOrderService {
    ExtraOrder createExtraOrder(Long customerId, Long productId, BigDecimal quantity, Date deliveryDate);
    ExtraOrder approveOrder(Long orderId);
    ExtraOrder rejectOrder(Long orderId);
    ExtraOrder getOrderById(Long orderId);
    List<ExtraOrder> getPendingOrders();
    List<ExtraOrder> getCustomerOrders(Long customerId);
    List<ExtraOrder> getOrdersByDeliveryDate(Date deliveryDate);
}
