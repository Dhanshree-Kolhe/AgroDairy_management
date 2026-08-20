package com.agrodairy.management.service.impl;

import com.agrodairy.management.dao.CustomerDAO;
import com.agrodairy.management.dao.ExtraOrderDAO;
import com.agrodairy.management.dao.MonthlyBillDAO;
import com.agrodairy.management.dao.impl.CustomerDAOImpl;
import com.agrodairy.management.dao.impl.ExtraOrderDAOImpl;
import com.agrodairy.management.dao.impl.MonthlyBillDAOImpl;
import com.agrodairy.management.exception.ResourceNotFoundException;
import com.agrodairy.management.exception.ValidationException;
import com.agrodairy.management.model.Customer;
import com.agrodairy.management.model.DeliveryRecord;
import com.agrodairy.management.model.ExtraOrder;
import com.agrodairy.management.model.MonthlyBill;
import com.agrodairy.management.model.enums.PaymentStatus;
import com.agrodairy.management.service.BillingService;
import com.agrodairy.management.service.DeliveryService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

public class BillingServiceImpl implements BillingService {
    private final MonthlyBillDAO monthlyBillDAO;
    private final CustomerDAO customerDAO;
    private final DeliveryService deliveryService;
    private final ExtraOrderDAO extraOrderDAO;

    public BillingServiceImpl() {
        this.monthlyBillDAO = new MonthlyBillDAOImpl();
        this.customerDAO = new CustomerDAOImpl();
        this.deliveryService = new DeliveryServiceImpl();
        this.extraOrderDAO = new ExtraOrderDAOImpl();
    }

    public BillingServiceImpl(MonthlyBillDAO monthlyBillDAO, CustomerDAO customerDAO, DeliveryService deliveryService, ExtraOrderDAO extraOrderDAO) {
        this.monthlyBillDAO = monthlyBillDAO;
        this.customerDAO = customerDAO;
        this.deliveryService = deliveryService;
        this.extraOrderDAO = extraOrderDAO;
    }

    @Override
    public MonthlyBill generateMonthlyBill(Long customerId, int month, int year) {
        if (customerId == null || customerId <= 0) throw new ValidationException("Invalid customer ID.");
        if (month < 1 || month > 12) throw new ValidationException("Invalid billing month (1-12).");
        if (year < 2000) throw new ValidationException("Invalid billing year.");

        Customer customer = customerDAO.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with ID: " + customerId));

        // Prevent Duplicate Bill creation for same customer and period
        Optional<MonthlyBill> existingOpt = monthlyBillDAO.findByCustomerAndPeriod(customerId, month, year);
        if (existingOpt.isPresent()) {
            return recalculateMonthlyBill(existingOpt.get().getBillId());
        }

        // 1. Calculate Milk Amount using stored rate snapshots from DeliveryRecords
        List<DeliveryRecord> deliveryRecords = deliveryService.getCustomerDeliveryHistory(customerId, month, year);
        BigDecimal totalMilkQuantity = deliveryRecords.stream()
                .filter(r -> r.getStatus() == com.agrodairy.management.model.enums.DeliveryStatus.DELIVERED)
                .map(DeliveryRecord::getDeliveredQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal milkCharges = deliveryRecords.stream()
                .filter(r -> r.getStatus() == com.agrodairy.management.model.enums.DeliveryStatus.DELIVERED)
                .map(r -> r.getDeliveredQuantity().multiply(r.getRatePerLiter())) // Uses STORED RATE SNAPSHOT
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        // 2. Calculate Extra Products Amount using stored price snapshots
        List<ExtraOrder> extraOrders = extraOrderDAO.findByCustomerAndMonth(customerId, month, year);
        BigDecimal extraOrdersCharges = extraOrders.stream()
                .map(ExtraOrder::getTotalPrice) // Uses STORED PRICE SNAPSHOT
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal holidayDeductions = BigDecimal.ZERO;

        // 3. Get Previous Unpaid Balance
        BigDecimal previousBalance = getPreviousBalanceForPeriod(customerId, month, year);

        BigDecimal grossAmount = milkCharges.add(extraOrdersCharges).subtract(holidayDeductions).setScale(2, RoundingMode.HALF_UP);
        BigDecimal netPayable = grossAmount.add(previousBalance).setScale(2, RoundingMode.HALF_UP);
        BigDecimal paidAmount = BigDecimal.ZERO;
        BigDecimal balanceDue = netPayable;

        YearMonth billingPeriod = YearMonth.of(year, month);
        LocalDate lastDayOfMonth = billingPeriod.atEndOfMonth();
        LocalDate dueLocalDate = lastDayOfMonth.plusDays(10); // 10 days grace period

        String billNumber = String.format("BILL-%04d%02d-%d", year, month, customerId);

        MonthlyBill bill = new MonthlyBill();
        bill.setBillNumber(billNumber);
        bill.setCustomerId(customerId);
        bill.setBillingMonth(month);
        bill.setBillingYear(year);
        bill.setTotalMilkQuantity(totalMilkQuantity);
        bill.setMilkCharges(milkCharges);
        bill.setExtraOrdersCharges(extraOrdersCharges);
        bill.setHolidayDeductions(holidayDeductions);
        bill.setGrossAmount(grossAmount);
        bill.setPreviousBalance(previousBalance);
        bill.setNetPayable(netPayable);
        bill.setPaidAmount(paidAmount);
        bill.setBalanceDue(balanceDue);
        bill.setStatus(PaymentStatus.UNPAID);
        bill.setBillDate(Date.valueOf(lastDayOfMonth));
        bill.setDueDate(Date.valueOf(dueLocalDate));

        Long billId = monthlyBillDAO.insert(bill);
        bill.setBillId(billId);
        return bill;
    }

    @Override
    public MonthlyBill getCustomerMonthlyBill(Long customerId, int month, int year) {
        if (customerId == null || customerId <= 0) throw new ValidationException("Invalid customer ID.");
        return monthlyBillDAO.findByCustomerAndPeriod(customerId, month, year)
                .orElseThrow(() -> new ResourceNotFoundException(String.format("Bill not found for customer %d period %02d/%d", customerId, month, year)));
    }

    @Override
    public List<MonthlyBill> getCustomerBillHistory(Long customerId) {
        if (customerId == null || customerId <= 0) throw new ValidationException("Invalid customer ID.");
        return monthlyBillDAO.findByCustomer(customerId);
    }

    @Override
    public List<MonthlyBill> getMonthlyBillsByPeriod(int month, int year) {
        if (month < 1 || month > 12) throw new ValidationException("Invalid month.");
        return monthlyBillDAO.findByPeriod(month, year);
    }

    @Override
    public List<MonthlyBill> getPendingBills() {
        return monthlyBillDAO.findPendingBills();
    }

    @Override
    public BigDecimal getOutstandingBalance(Long customerId) {
        if (customerId == null || customerId <= 0) throw new ValidationException("Invalid customer ID.");
        List<MonthlyBill> bills = monthlyBillDAO.findByCustomer(customerId);
        return bills.stream()
                .map(MonthlyBill::getBalanceDue)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public MonthlyBill recalculateMonthlyBill(Long billId) {
        if (billId == null || billId <= 0) throw new ValidationException("Invalid bill ID.");
        MonthlyBill bill = monthlyBillDAO.findById(billId)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found with ID: " + billId));

        int month = bill.getBillingMonth();
        int year = bill.getBillingYear();
        Long customerId = bill.getCustomerId();

        List<DeliveryRecord> deliveryRecords = deliveryService.getCustomerDeliveryHistory(customerId, month, year);
        BigDecimal totalMilkQuantity = deliveryRecords.stream()
                .filter(r -> r.getStatus() == com.agrodairy.management.model.enums.DeliveryStatus.DELIVERED)
                .map(DeliveryRecord::getDeliveredQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal milkCharges = deliveryRecords.stream()
                .filter(r -> r.getStatus() == com.agrodairy.management.model.enums.DeliveryStatus.DELIVERED)
                .map(r -> r.getDeliveredQuantity().multiply(r.getRatePerLiter())) // STORED RATE SNAPSHOT
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        List<ExtraOrder> extraOrders = extraOrderDAO.findByCustomerAndMonth(customerId, month, year);
        BigDecimal extraOrdersCharges = extraOrders.stream()
                .map(ExtraOrder::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal grossAmount = milkCharges.add(extraOrdersCharges).subtract(bill.getHolidayDeductions()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal netPayable = grossAmount.add(bill.getPreviousBalance()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal balanceDue = netPayable.subtract(bill.getPaidAmount()).setScale(2, RoundingMode.HALF_UP);

        PaymentStatus status = PaymentStatus.UNPAID;
        if (balanceDue.compareTo(BigDecimal.ZERO) <= 0) {
            status = PaymentStatus.PAID;
            balanceDue = BigDecimal.ZERO;
        } else if (bill.getPaidAmount().compareTo(BigDecimal.ZERO) > 0) {
            status = PaymentStatus.PARTIAL;
        }

        bill.setTotalMilkQuantity(totalMilkQuantity);
        bill.setMilkCharges(milkCharges);
        bill.setExtraOrdersCharges(extraOrdersCharges);
        bill.setGrossAmount(grossAmount);
        bill.setNetPayable(netPayable);
        bill.setBalanceDue(balanceDue);
        bill.setStatus(status);

        monthlyBillDAO.update(bill);
        return bill;
    }

    private BigDecimal getPreviousBalanceForPeriod(Long customerId, int currentMonth, int currentYear) {
        YearMonth currentPeriod = YearMonth.of(currentYear, currentMonth);
        YearMonth prevPeriod = currentPeriod.minusMonths(1);
        Optional<MonthlyBill> prevBillOpt = monthlyBillDAO.findByCustomerAndPeriod(customerId, prevPeriod.getMonthValue(), prevPeriod.getYear());
        return prevBillOpt.map(MonthlyBill::getBalanceDue).orElse(BigDecimal.ZERO);
    }
}
