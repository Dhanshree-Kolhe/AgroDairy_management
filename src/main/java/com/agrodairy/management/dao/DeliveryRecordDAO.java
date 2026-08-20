package com.agrodairy.management.dao;

import com.agrodairy.management.model.DeliveryRecord;
import com.agrodairy.management.model.enums.DeliveryShift;
import com.agrodairy.management.model.enums.DeliveryStatus;

import java.sql.Date;
import java.util.List;
import java.util.Optional;

public interface DeliveryRecordDAO {
    Long insert(DeliveryRecord deliveryRecord);
    boolean updateStatus(Long deliveryId, DeliveryStatus status, String remarks);
    boolean saveBatchDeliveries(List<DeliveryRecord> deliveryRecords);
    Optional<DeliveryRecord> findById(Long deliveryId);
    Optional<DeliveryRecord> findByCustomerDateShift(Long customerId, Date deliveryDate, DeliveryShift shift);
    List<DeliveryRecord> findByDateAndShift(Date deliveryDate, DeliveryShift shift);
    List<DeliveryRecord> findByCustomerAndMonth(Long customerId, int month, int year);
    List<DeliveryRecord> findByDate(Date deliveryDate);
}
