package com.agrodairy.management.service;

import com.agrodairy.management.dao.CustomerDAO;
import com.agrodairy.management.dao.DeliveryRecordDAO;
import com.agrodairy.management.dao.HolidayRequestDAO;
import com.agrodairy.management.exception.ValidationException;
import com.agrodairy.management.model.Customer;
import com.agrodairy.management.model.DeliveryRecord;
import com.agrodairy.management.model.HolidayRequest;
import com.agrodairy.management.model.enums.DeliveryShift;
import com.agrodairy.management.model.enums.DeliveryStatus;
import com.agrodairy.management.model.enums.MilkType;
import com.agrodairy.management.model.enums.SubscriptionStatus;
import com.agrodairy.management.service.impl.DeliveryServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class DeliveryServiceTest {

    private DeliveryService deliveryService;
    private StubDeliveryRecordDAO deliveryRecordDAO;
    private StubCustomerDAO customerDAO;
    private StubHolidayRequestDAO holidayRequestDAO;

    @BeforeEach
    public void setUp() {
        deliveryRecordDAO = new StubDeliveryRecordDAO();
        customerDAO = new StubCustomerDAO();
        holidayRequestDAO = new StubHolidayRequestDAO();

        deliveryService = new DeliveryServiceImpl(deliveryRecordDAO, customerDAO, holidayRequestDAO);

        Customer c = new Customer();
        c.setCustomerId(101L);
        c.setVendorId(1L);
        c.setCustomerCode("CUST-101");
        c.setFullName("Rahul Verma");
        c.setPhone("9876500001");
        c.setMilkType(MilkType.COW);
        c.setMorningQuantity(new BigDecimal("1.50"));
        c.setEveningQuantity(new BigDecimal("1.00"));
        c.setDeliveryShift(DeliveryShift.BOTH);
        c.setCurrentPricePerLiter(new BigDecimal("60.00"));
        c.setStatus(SubscriptionStatus.ACTIVE);

        customerDAO.insert(c);
    }

    @Test
    public void test4_DeliveryMarkingAndRateSnapshot() {
        Date today = new Date(System.currentTimeMillis());

        DeliveryRecord record = deliveryService.markDelivered(101L, today, DeliveryShift.MORNING, "Delivered on time");
        assertNotNull(record.getDeliveryId());
        assertEquals(new BigDecimal("1.50"), record.getDeliveredQuantity());
        assertEquals(new BigDecimal("60.00"), record.getRatePerLiter()); // RATE SNAPSHOT
        assertEquals(new BigDecimal("90.00"), record.getTotalAmount()); // 1.5 * 60

        // Change customer current price in database
        Customer c = customerDAO.findById(101L).get();
        c.setCurrentPricePerLiter(new BigDecimal("75.00"));

        // Historical record MUST retain stored ratePerLiter of 60.00
        assertEquals(new BigDecimal("60.00"), record.getRatePerLiter());
    }

    @Test
    public void test5_DuplicateDeliveryPreventionAndUpdate() {
        Date today = new Date(System.currentTimeMillis());

        DeliveryRecord record1 = deliveryService.markDelivered(101L, today, DeliveryShift.MORNING, "First mark");
        DeliveryRecord record2 = deliveryService.markDelivered(101L, today, DeliveryShift.MORNING, "Updated mark");

        // Should update existing record instead of creating duplicate ID
        assertEquals(record1.getDeliveryId(), record2.getDeliveryId());
        assertEquals("Updated mark", record2.getRemarks());
    }

    private static class StubDeliveryRecordDAO implements DeliveryRecordDAO {
        private final List<DeliveryRecord> list = new ArrayList<>();
        private long seq = 1;

        @Override public Long insert(DeliveryRecord r) { r.setDeliveryId(seq++); list.add(r); return r.getDeliveryId(); }
        @Override public boolean updateStatus(Long id, DeliveryStatus status, String remarks) { return true; }
        @Override public boolean saveBatchDeliveries(List<DeliveryRecord> records) { list.addAll(records); return true; }
        @Override public Optional<DeliveryRecord> findById(Long id) { return list.stream().filter(r -> r.getDeliveryId().equals(id)).findFirst(); }
        @Override public Optional<DeliveryRecord> findByCustomerDateShift(Long customerId, Date date, DeliveryShift shift) {
            return list.stream().filter(r -> r.getCustomerId().equals(customerId) && r.getDeliveryDate().toString().equals(date.toString()) && r.getDeliveryShift() == shift).findFirst();
        }
        @Override public List<DeliveryRecord> findByDateAndShift(Date date, DeliveryShift shift) { return list; }
        @Override public List<DeliveryRecord> findByCustomerAndMonth(Long customerId, int month, int year) { return list; }
        @Override public List<DeliveryRecord> findByDate(Date date) { return list; }
    }

    private static class StubCustomerDAO implements CustomerDAO {
        private final List<Customer> customers = new ArrayList<>();
        @Override public Long insert(Customer c) { customers.add(c); return c.getCustomerId(); }
        @Override public boolean update(Customer c) { return true; }
        @Override public boolean updateStatus(Long id, SubscriptionStatus status) { return true; }
        @Override public boolean softDelete(Long id) { return true; }
        @Override public Optional<Customer> findById(Long id) { return customers.stream().filter(c -> c.getCustomerId().equals(id)).findFirst(); }
        @Override public Optional<Customer> findByCustomerCode(String code) { return customers.stream().filter(c -> c.getCustomerCode().equals(code)).findFirst(); }
        @Override public Optional<Customer> findByUserId(Long userId) { return Optional.empty(); }
        @Override public List<Customer> findAll() { return customers; }
        @Override public List<Customer> findByVendorId(Long vendorId) { return customers; }
        @Override public List<Customer> findByArea(String area) { return customers; }
        @Override public List<Customer> search(String query) { return customers; }
    }

    private static class StubHolidayRequestDAO implements HolidayRequestDAO {
        @Override public Long insert(HolidayRequest r) { return 1L; }
        @Override public boolean updateStatus(Long id, com.agrodairy.management.model.enums.RequestStatus status, String remarks) { return true; }
        @Override public Optional<HolidayRequest> findById(Long id) { return Optional.empty(); }
        @Override public List<HolidayRequest> findByCustomerId(Long id) { return new ArrayList<>(); }
        @Override public List<HolidayRequest> findPendingRequests() { return new ArrayList<>(); }
        @Override public boolean isCustomerOnHoliday(Long id, Date date) { return false; }
        @Override public List<HolidayRequest> findApprovedHolidaysForPeriod(Long id, Date s, Date e) { return new ArrayList<>(); }
    }
}
