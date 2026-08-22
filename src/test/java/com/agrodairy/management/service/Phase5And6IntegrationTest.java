package com.agrodairy.management.service;

import com.agrodairy.management.controller.RoutePlannerController;
import com.agrodairy.management.model.*;
import com.agrodairy.management.model.enums.*;
import com.agrodairy.management.service.impl.*;
import com.agrodairy.management.util.ExcelReportExporter;
import com.agrodairy.management.util.PdfInvoiceGenerator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.math.BigDecimal;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class Phase5And6IntegrationTest {

    private KnowledgeAssistantService knowledgeService;
    private CloudBackupService cloudBackupService;

    @BeforeEach
    public void setUp() {
        this.knowledgeService = new KnowledgeAssistantServiceImpl();
        this.cloudBackupService = new FirebaseBackupServiceImpl();
    }

    @Test
    @DisplayName("Phase 5.1 — Route Planner Haversine & Dijkstra Calculation Test")
    public void testRoutePlannerHaversineCalculation() {
        // Dharampeth HQ (21.1415, 79.0634) to Sadar (21.1610, 79.0805)
        double distanceKm = RoutePlannerController.calculateHaversineKm(21.1415, 79.0634, 21.1610, 79.0805);
        assertTrue(distanceKm > 1.5 && distanceKm < 4.0, "Haversine distance between Dharampeth and Sadar should be ~2.8 km");

        // Same location distance should be 0.0
        double sameDist = RoutePlannerController.calculateHaversineKm(21.1415, 79.0634, 21.1415, 79.0634);
        assertEquals(0.0, sameDist, 0.001, "Distance to same point should be 0");
    }

    @Test
    @DisplayName("Phase 5.2 — Offline Knowledge Assistant JSON & Keyword Search Test")
    public void testKnowledgeAssistantOfflineFunctionality() {
        List<FAQKnowledgeBase> allFaqs = knowledgeService.getAllFAQs();
        assertNotNull(allFaqs, "FAQ list should not be null");
        assertFalse(allFaqs.isEmpty(), "FAQ list should load entries from JSON resource");

        // Test search query for Milk Storage
        FAQKnowledgeBase matchBoil = knowledgeService.getBestMatchingFAQ("boil milk storage");
        assertNotNull(matchBoil, "Best matching FAQ should not be null");
        assertTrue(matchBoil.getQuestion().toLowerCase().contains("boil") || matchBoil.getAnswer().toLowerCase().contains("boil"), "Matched FAQ should contain boiling guidelines");

        // Test search for Paneer
        FAQKnowledgeBase matchPaneer = knowledgeService.getBestMatchingFAQ("paneer shelf life fridge");
        assertNotNull(matchPaneer);
        assertTrue(matchPaneer.getAnswer().toLowerCase().contains("paneer") || matchPaneer.getQuestion().toLowerCase().contains("paneer"));

        // Test search for Curd Storage
        FAQKnowledgeBase matchCurd = knowledgeService.getBestMatchingFAQ("curd storage dahi");
        assertNotNull(matchCurd);
        assertTrue(matchCurd.getAnswer().toLowerCase().contains("curd") || matchCurd.getCategory().toLowerCase().contains("curd"));

        // Test search for Cattle Feeding
        FAQKnowledgeBase matchFeeding = knowledgeService.getBestMatchingFAQ("cattle feeding fodder ratio");
        assertNotNull(matchFeeding);
        assertTrue(matchFeeding.getAnswer().toLowerCase().contains("fodder") || matchFeeding.getCategory().toLowerCase().contains("feeding"));
    }

    @Test
    @DisplayName("Phase 5.3 — Product Units Enum Standardisation Test")
    public void testProductUnitsStandardisation() {
        Product p1 = new Product(1L, "PRD-101", "Fresh Milk", ProductCategory.MILK, ProductUnit.LITER, new BigDecimal("60.00"), "ACTIVE");
        Product p2 = new Product(2L, "PRD-103", "Paneer", ProductCategory.DAIRY_PRODUCT, ProductUnit.KG, new BigDecimal("360.00"), "ACTIVE");
        Product p3 = new Product(3L, "PRD-106", "Butter", ProductCategory.DAIRY_PRODUCT, ProductUnit.GRAM, new BigDecimal("280.00"), "ACTIVE");
        Product p4 = new Product(4L, "PRD-107", "Cheese", ProductCategory.DAIRY_PRODUCT, ProductUnit.PACKET, new BigDecimal("140.00"), "ACTIVE");

        assertEquals(ProductUnit.LITER, p1.getUnit());
        assertEquals(ProductUnit.KG, p2.getUnit());
        assertEquals(ProductUnit.GRAM, p3.getUnit());
        assertEquals(ProductUnit.PACKET, p4.getUnit());
    }

    @Test
    @DisplayName("Phase 6.1 — Optional Firebase Cloud Backup Graceful Fallback Test")
    public void testCloudBackupServiceFallback() {
        assertNotNull(cloudBackupService);
        String status = cloudBackupService.getCloudBackupStatus();
        assertNotNull(status);
        assertFalse(status.isEmpty());

        // Calling sync in unconfigured test env should safely return false without throwing exception
        boolean result = cloudBackupService.syncLocalDataToCloud();
        assertFalse(result, "Unconfigured cloud sync should return false gracefully");
    }

    @Test
    @DisplayName("Phase 6.2 — PDF Invoice Generation Test")
    public void testPdfInvoiceGeneration() throws Exception {
        MonthlyBill bill = new MonthlyBill();
        bill.setBillNumber("BILL-TEST-202608-101");
        bill.setCustomerId(1L);
        bill.setBillingMonth(8);
        bill.setBillingYear(2026);
        bill.setTotalMilkQuantity(new BigDecimal("45.00"));
        bill.setMilkCharges(new BigDecimal("2700.00"));
        bill.setExtraOrdersCharges(new BigDecimal("360.00"));
        bill.setHolidayDeductions(BigDecimal.ZERO);
        bill.setGrossAmount(new BigDecimal("3060.00"));
        bill.setPreviousBalance(BigDecimal.ZERO);
        bill.setNetPayable(new BigDecimal("3060.00"));
        bill.setPaidAmount(new BigDecimal("3060.00"));
        bill.setBalanceDue(BigDecimal.ZERO);
        bill.setStatus(PaymentStatus.PAID);
        bill.setBillDate(Date.valueOf("2026-08-31"));
        bill.setDueDate(Date.valueOf("2026-09-10"));

        Customer customer = new Customer();
        customer.setCustomerId(1L);
        customer.setCustomerCode("CUST-1001");
        customer.setFullName("Rahul Verma");
        customer.setPhone("9876500001");
        customer.setAddress("Flat 402, Dharampeth, Nagpur");

        List<DeliveryRecord> deliveries = new ArrayList<>();
        deliveries.add(new DeliveryRecord(1L, 1L, Date.valueOf("2026-08-01"), DeliveryShift.MORNING, new BigDecimal("1.50"), new BigDecimal("60.00"), new BigDecimal("90.00"), DeliveryStatus.DELIVERED, "Delivered"));

        List<ExtraOrder> extraOrders = new ArrayList<>();
        Product paneer = new Product(3L, "PRD-103", "Fresh Paneer", ProductCategory.DAIRY_PRODUCT, ProductUnit.KG, new BigDecimal("360.00"), "ACTIVE");
        ExtraOrder eo = new ExtraOrder(1L, 1L, 3L, new BigDecimal("1.00"), new BigDecimal("360.00"), new BigDecimal("360.00"), Date.valueOf("2026-08-01"), Date.valueOf("2026-08-02"), RequestStatus.DELIVERED);
        eo.setProduct(paneer);
        extraOrders.add(eo);

        List<Payment> payments = new ArrayList<>();
        payments.add(new Payment(1L, 1L, 1L, new BigDecimal("3060.00"), Date.valueOf("2026-08-05"), PaymentMode.UPI, "UPI/TEST/123", "SUCCESS"));

        File tempPdf = File.createTempFile("test_invoice_", ".pdf");
        tempPdf.deleteOnExit();

        PdfInvoiceGenerator.generateMonthlyInvoicePdf(bill, customer, deliveries, extraOrders, payments, tempPdf);

        assertTrue(tempPdf.exists(), "PDF file should be created");
        assertTrue(tempPdf.length() > 0, "PDF file should not be empty");
    }

    @Test
    @DisplayName("Phase 6.3 — Excel Report Export Test")
    public void testExcelReportExport() throws Exception {
        List<MonthlyBill> bills = new ArrayList<>();
        MonthlyBill bill = new MonthlyBill();
        bill.setBillNumber("BILL-TEST-202608-101");
        bill.setCustomerId(1L);
        bill.setBillingMonth(8);
        bill.setBillingYear(2026);
        bill.setTotalMilkQuantity(new BigDecimal("45.00"));
        bill.setMilkCharges(new BigDecimal("2700.00"));
        bill.setExtraOrdersCharges(new BigDecimal("360.00"));
        bill.setHolidayDeductions(BigDecimal.ZERO);
        bill.setGrossAmount(new BigDecimal("3060.00"));
        bill.setPreviousBalance(BigDecimal.ZERO);
        bill.setNetPayable(new BigDecimal("3060.00"));
        bill.setPaidAmount(new BigDecimal("3060.00"));
        bill.setBalanceDue(BigDecimal.ZERO);
        bill.setStatus(PaymentStatus.PAID);
        bill.setBillDate(Date.valueOf("2026-08-31"));
        bill.setDueDate(Date.valueOf("2026-09-10"));
        bills.add(bill);

        Customer customer = new Customer();
        customer.setCustomerId(1L);
        customer.setCustomerCode("CUST-1001");
        customer.setFullName("Rahul Verma");
        customer.setArea("Dharampeth");

        java.util.Map<Long, Customer> custMap = java.util.Map.of(1L, customer);

        File tempExcel = File.createTempFile("test_ledger_", ".xlsx");
        tempExcel.deleteOnExit();

        ExcelReportExporter.exportFinancialRecordsToExcel(bills, custMap, tempExcel);

        assertTrue(tempExcel.exists(), "Excel .xlsx file should be created");
        assertTrue(tempExcel.length() > 0, "Excel .xlsx file should not be empty");
    }
}
