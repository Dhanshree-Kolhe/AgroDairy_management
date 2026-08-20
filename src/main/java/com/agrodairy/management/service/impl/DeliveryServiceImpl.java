package com.agrodairy.management.service.impl;

import com.agrodairy.management.dao.CustomerDAO;
import com.agrodairy.management.dao.DeliveryRecordDAO;
import com.agrodairy.management.dao.HolidayRequestDAO;
import com.agrodairy.management.dao.impl.CustomerDAOImpl;
import com.agrodairy.management.dao.impl.DeliveryRecordDAOImpl;
import com.agrodairy.management.dao.impl.HolidayRequestDAOImpl;
import com.agrodairy.management.exception.DuplicateDeliveryException;
import com.agrodairy.management.exception.ResourceNotFoundException;
import com.agrodairy.management.exception.ValidationException;
import com.agrodairy.management.model.Customer;
import com.agrodairy.management.model.DeliveryRecord;
import com.agrodairy.management.model.enums.DeliveryShift;
import com.agrodairy.management.model.enums.DeliveryStatus;
import com.agrodairy.management.service.DeliveryService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.util.List;
import java.util.Optional;

public class DeliveryServiceImpl implements DeliveryService {
    private final DeliveryRecordDAO deliveryRecordDAO;
    private final CustomerDAO customerDAO;
    private final HolidayRequestDAO holidayRequestDAO;

    public DeliveryServiceImpl() {
        this.deliveryRecordDAO = new DeliveryRecordDAOImpl();
        this.customerDAO = new CustomerDAOImpl();
        this.holidayRequestDAO = new HolidayRequestDAOImpl();
    }

    public DeliveryServiceImpl(DeliveryRecordDAO deliveryRecordDAO, CustomerDAO customerDAO, HolidayRequestDAO holidayRequestDAO) {
        this.deliveryRecordDAO = deliveryRecordDAO;
        this.customerDAO = customerDAO;
        this.holidayRequestDAO = holidayRequestDAO;
    }

    @Override
    public DeliveryRecord markDelivery(Long customerId, Date deliveryDate, DeliveryShift shift, DeliveryStatus status, BigDecimal overrideQuantity, String remarks) {
        if (customerId == null || customerId <= 0) throw new ValidationException("Customer ID is required.");
        if (deliveryDate == null) throw new ValidationException("Delivery date is required.");
        if (shift == null) throw new ValidationException("Delivery shift is required.");
        if (status == null) throw new ValidationException("Delivery status is required.");

        Customer customer = customerDAO.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with ID: " + customerId));

        // Check if customer is on an approved holiday pause
        boolean isOnHoliday = holidayRequestDAO.isCustomerOnHoliday(customerId, deliveryDate);
        if (isOnHoliday && status == DeliveryStatus.DELIVERED) {
            status = DeliveryStatus.PAUSED;
            remarks = (remarks != null ? remarks + " " : "") + "[Auto Paused: Customer on Approved Holiday]";
        }

        // Prevent Duplicate Delivery Record creation
        Optional<DeliveryRecord> existingOpt = deliveryRecordDAO.findByCustomerDateShift(customerId, deliveryDate, shift);
        if (existingOpt.isPresent()) {
            // Update existing record
            DeliveryRecord existing = existingOpt.get();
            BigDecimal rateSnapshot = customer.getCurrentPricePerLiter();
            existing.setRatePerLiter(rateSnapshot); // Refresh rate snapshot if re-marking
            existing.setStatus(status);

            BigDecimal qty = BigDecimal.ZERO;
            if (status == DeliveryStatus.DELIVERED) {
                qty = (overrideQuantity != null) ? overrideQuantity :
                        (shift == DeliveryShift.MORNING ? customer.getMorningQuantity() : customer.getEveningQuantity());
            }
            if (qty.compareTo(BigDecimal.ZERO) < 0) throw new ValidationException("Quantity cannot be negative.");

            existing.setDeliveredQuantity(qty);
            existing.setTotalAmount(qty.multiply(rateSnapshot).setScale(2, RoundingMode.HALF_UP));
            existing.setRemarks(remarks);

            deliveryRecordDAO.updateStatus(existing.getDeliveryId(), status, remarks);
            return existing;
        }

        // Create new delivery record with IMMUTABLE RATE SNAPSHOT
        BigDecimal rateSnapshot = customer.getCurrentPricePerLiter();
        BigDecimal qty = BigDecimal.ZERO;
        if (status == DeliveryStatus.DELIVERED) {
            qty = (overrideQuantity != null) ? overrideQuantity :
                    (shift == DeliveryShift.MORNING ? customer.getMorningQuantity() : customer.getEveningQuantity());
        }
        if (qty.compareTo(BigDecimal.ZERO) < 0) throw new ValidationException("Quantity cannot be negative.");

        BigDecimal totalAmount = qty.multiply(rateSnapshot).setScale(2, RoundingMode.HALF_UP);

        DeliveryRecord record = new DeliveryRecord();
        record.setCustomerId(customerId);
        record.setDeliveryDate(deliveryDate);
        record.setDeliveryShift(shift);
        record.setDeliveredQuantity(qty);
        record.setRatePerLiter(rateSnapshot); // CRITICAL RATE SNAPSHOT PRESERVATION
        record.setTotalAmount(totalAmount);
        record.setStatus(status);
        record.setRemarks(remarks);

        Long id = deliveryRecordDAO.insert(record);
        record.setDeliveryId(id);
        return record;
    }

    @Override
    public DeliveryRecord markDelivered(Long customerId, Date deliveryDate, DeliveryShift shift, String remarks) {
        return markDelivery(customerId, deliveryDate, shift, DeliveryStatus.DELIVERED, null, remarks);
    }

    @Override
    public DeliveryRecord markNotDelivered(Long customerId, Date deliveryDate, DeliveryShift shift, String remarks) {
        return markDelivery(customerId, deliveryDate, shift, DeliveryStatus.NOT_DELIVERED, BigDecimal.ZERO, remarks);
    }

    @Override
    public DeliveryRecord markPaused(Long customerId, Date deliveryDate, DeliveryShift shift, String remarks) {
        return markDelivery(customerId, deliveryDate, shift, DeliveryStatus.PAUSED, BigDecimal.ZERO, remarks);
    }

    @Override
    public List<DeliveryRecord> getTodayDeliveries(Date date, DeliveryShift shift) {
        if (date == null) throw new ValidationException("Date is required.");
        if (shift == null) throw new ValidationException("Shift is required.");
        return deliveryRecordDAO.findByDateAndShift(date, shift);
    }

    @Override
    public List<DeliveryRecord> getCustomerDeliveryHistory(Long customerId, int month, int year) {
        if (customerId == null || customerId <= 0) throw new ValidationException("Invalid customer ID.");
        if (month < 1 || month > 12) throw new ValidationException("Invalid month (1-12).");
        if (year < 2000) throw new ValidationException("Invalid year.");
        return deliveryRecordDAO.findByCustomerAndMonth(customerId, month, year);
    }

    @Override
    public List<DeliveryRecord> getDeliveriesByDate(Date date) {
        if (date == null) throw new ValidationException("Date is required.");
        return deliveryRecordDAO.findByDate(date);
    }

    @Override
    public BigDecimal calculateDeliveredQuantity(Long customerId, int month, int year) {
        List<DeliveryRecord> records = getCustomerDeliveryHistory(customerId, month, year);
        return records.stream()
                .filter(r -> r.getStatus() == DeliveryStatus.DELIVERED)
                .map(DeliveryRecord::getDeliveredQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public BigDecimal calculateDeliveryAmount(Long customerId, int month, int year) {
        List<DeliveryRecord> records = getCustomerDeliveryHistory(customerId, month, year);
        return records.stream()
                .filter(r -> r.getStatus() == DeliveryStatus.DELIVERED)
                .map(DeliveryRecord::getTotalAmount) // Uses historical stored totalAmount (qty * ratePerLiter snapshot)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public boolean saveBatchDeliveries(List<DeliveryRecord> records) {
        if (records == null || records.isEmpty()) return true;
        for (DeliveryRecord r : records) {
            if (r.getRatePerLiter() == null || r.getRatePerLiter().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ValidationException("Delivery record rate per liter snapshot cannot be null or zero.");
            }
            if (r.getDeliveredQuantity() == null || r.getDeliveredQuantity().compareTo(BigDecimal.ZERO) < 0) {
                throw new ValidationException("Delivered quantity cannot be negative.");
            }
            r.setTotalAmount(r.getDeliveredQuantity().multiply(r.getRatePerLiter()).setScale(2, RoundingMode.HALF_UP));
        }
        return deliveryRecordDAO.saveBatchDeliveries(records);
    }
}
