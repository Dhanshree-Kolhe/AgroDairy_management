package com.agrodairy.management.service;

import com.agrodairy.management.model.DeliveryRecord;
import com.agrodairy.management.model.enums.DeliveryShift;
import com.agrodairy.management.model.enums.DeliveryStatus;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.List;

public interface DeliveryService {
    DeliveryRecord markDelivery(Long customerId, Date deliveryDate, DeliveryShift shift, DeliveryStatus status, BigDecimal overrideQuantity, String remarks);
    DeliveryRecord markDelivered(Long customerId, Date deliveryDate, DeliveryShift shift, String remarks);
    DeliveryRecord markNotDelivered(Long customerId, Date deliveryDate, DeliveryShift shift, String remarks);
    DeliveryRecord markPaused(Long customerId, Date deliveryDate, DeliveryShift shift, String remarks);
    
    List<DeliveryRecord> getTodayDeliveries(Date date, DeliveryShift shift);
    List<DeliveryRecord> getCustomerDeliveryHistory(Long customerId, int month, int year);
    List<DeliveryRecord> getDeliveriesByDate(Date date);
    
    BigDecimal calculateDeliveredQuantity(Long customerId, int month, int year);
    BigDecimal calculateDeliveryAmount(Long customerId, int month, int year);
    boolean saveBatchDeliveries(List<DeliveryRecord> records);
}
