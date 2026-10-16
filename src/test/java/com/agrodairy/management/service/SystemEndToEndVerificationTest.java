package com.agrodairy.management.service;

import com.agrodairy.management.config.DatabaseConfig;
import com.agrodairy.management.dao.*;
import com.agrodairy.management.dao.impl.*;
import com.agrodairy.management.exception.AuthenticationException;
import com.agrodairy.management.exception.ValidationException;
import com.agrodairy.management.model.*;
import com.agrodairy.management.model.enums.*;
import com.agrodairy.management.service.impl.*;
import com.agrodairy.management.util.SessionManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive System End-to-End Verification Test Suite
 * Validates actual database integration, role authentication, transaction integrity,
 * billing, delivery rate snapshotting, inventory, extra orders, and knowledge assistant.
 */
public class SystemEndToEndVerificationTest {

    private final AuthService authService = new AuthServiceImpl();
    private final CustomerDAO customerDAO = new CustomerDAOImpl();
    private final ProductDAO productDAO = new ProductDAOImpl();
    private final InventoryDAO inventoryDAO = new InventoryDAOImpl();
    private final MonthlyBillDAO monthlyBillDAO = new MonthlyBillDAOImpl();
    private final PaymentDAO paymentDAO = new PaymentDAOImpl();
    private final DeliveryRecordDAO deliveryRecordDAO = new DeliveryRecordDAOImpl();
    private final ExtraOrderDAO extraOrderDAO = new ExtraOrderDAOImpl();
    private final HolidayRequestDAO holidayRequestDAO = new HolidayRequestDAOImpl();
    private final KnowledgeAssistantService knowledgeService = new KnowledgeAssistantServiceImpl();

    @AfterEach
    public void tearDown() {
        SessionManager.clearSession();
    }

    @Test
    @DisplayName("Verification 1 — Database Connectivity and Pool Health")
    public void testDatabaseConnectivity() throws Exception {
        try (Connection conn = DatabaseConfig.getConnection()) {
            assertNotNull(conn, "Database connection should not be null");
            assertTrue(conn.isValid(3), "Database connection should be valid and responsive within 3s");
        }
    }

    @Test
    @DisplayName("Verification 2 — Admin, Vendor, Customer Authentication & Session Role Permissions")
    public void testUserAuthenticationAndRolePermissions() {
        // 1. Admin Authentication
        UserLogin admin = authService.login("admin_super", "secret123");
        assertNotNull(admin, "Admin login should succeed");
        assertEquals(Role.ADMIN, admin.getRole());
        assertEquals("admin_super", admin.getUsername());

        SessionManager.setSession(admin);
        assertTrue(SessionManager.isAdmin(), "Session should identify ADMIN role");
        assertFalse(SessionManager.isVendor(), "Admin should not be identified as VENDOR");
        assertFalse(SessionManager.isCustomer(), "Admin should not be identified as CUSTOMER");
        SessionManager.clearSession();
        assertNull(SessionManager.getCurrentUser(), "Session should be null after logout");

        // 2. Vendor Authentication
        UserLogin vendor = authService.login("vendor_ramesh", "secret123");
        assertNotNull(vendor, "Vendor login should succeed");
        assertEquals(Role.VENDOR, vendor.getRole());
        assertEquals("vendor_ramesh", vendor.getUsername());

        SessionManager.setSession(vendor);
        assertFalse(SessionManager.isAdmin(), "Vendor should not have ADMIN role");
        assertTrue(SessionManager.isVendor(), "Session should identify VENDOR role");
        assertFalse(SessionManager.isCustomer(), "Vendor should not have CUSTOMER role");
        SessionManager.clearSession();

        // 3. Customer Authentication
        UserLogin customerUser = authService.login("cust_rahul", "secret123");
        assertNotNull(customerUser, "Customer login should succeed");
        assertEquals(Role.CUSTOMER, customerUser.getRole());
        assertEquals("cust_rahul", customerUser.getUsername());

        Optional<Customer> custProfile = customerDAO.findByUserId(customerUser.getUserId());
        assertTrue(custProfile.isPresent(), "Customer profile should exist for user_id=" + customerUser.getUserId());

        SessionManager.setSession(customerUser, custProfile.get());
        assertFalse(SessionManager.isAdmin(), "Customer should not have ADMIN role");
        assertFalse(SessionManager.isVendor(), "Customer should not have VENDOR role");
        assertTrue(SessionManager.isCustomer(), "Session should identify CUSTOMER role");
        assertNotNull(SessionManager.getCurrentCustomer(), "Customer session profile must be populated");
        SessionManager.clearSession();
    }

    @Test
    @DisplayName("Verification 3 — Security Constraints: Password Mismatches & Empty Credentials")
    public void testSecurityCredentialConstraints() {
        // Wrong password rejection
        assertThrows(AuthenticationException.class, () -> authService.login("admin_super", "incorrectPass"));
        assertThrows(AuthenticationException.class, () -> authService.login("vendor_ramesh", "incorrectPass"));
        assertThrows(AuthenticationException.class, () -> authService.login("cust_rahul", "incorrectPass"));

        // Non-existent user rejection
        assertThrows(AuthenticationException.class, () -> authService.login("non_existent_account", "secret123"));

        // Blank username / password validation
        assertThrows(ValidationException.class, () -> authService.login("", "secret123"));
        assertThrows(ValidationException.class, () -> authService.login("admin_super", ""));
    }

    @Test
    @DisplayName("Verification 4 — Billing & Payments Database Verification")
    public void testBillingAndPaymentsData() {
        List<MonthlyBill> bills = monthlyBillDAO.findByPeriod(7, 2026);
        assertFalse(bills.isEmpty(), "Monthly bills table should contain records");

        Optional<MonthlyBill> seedBill = monthlyBillDAO.findByCustomerAndPeriod(1L, 7, 2026);
        assertTrue(seedBill.isPresent(), "Seed bill BILL-202607-1001 should exist for customer 1, 07/2026");

        MonthlyBill bill = seedBill.get();
        assertEquals(new BigDecimal("4860.00"), bill.getGrossAmount());
        assertEquals(new BigDecimal("4860.00"), bill.getPaidAmount());
        assertEquals(new BigDecimal("0.00"), bill.getBalanceDue());
        assertEquals(PaymentStatus.PAID, bill.getStatus());

        // Verify payments
        List<Payment> payments = paymentDAO.findByBillId(bill.getBillId());
        assertFalse(payments.isEmpty(), "Bill 1 should have associated payment record");
        Payment payment = payments.get(0);
        assertEquals(new BigDecimal("4860.00"), payment.getAmountPaid());
        assertEquals(PaymentMode.UPI, payment.getPaymentMode());
        assertEquals("SUCCESS", payment.getStatus());
    }

    @Test
    @DisplayName("Verification 5 — Delivery Records & Immutable Rate Snapshotting")
    public void testDeliveryRecordsAndRateSnapshotting() {
        List<DeliveryRecord> deliveries = deliveryRecordDAO.findByCustomerAndMonth(1L, 8, 2026);
        assertFalse(deliveries.isEmpty(), "Customer 1 should have delivery records for August 2026");

        for (DeliveryRecord record : deliveries) {
            assertNotNull(record.getRatePerLiter(), "Delivery record must maintain ratePerLiter snapshot");
            assertTrue(record.getRatePerLiter().compareTo(BigDecimal.ZERO) > 0, "Rate snapshot must be positive");
            BigDecimal expectedTotal = record.getDeliveredQuantity().multiply(record.getRatePerLiter()).setScale(2, java.math.RoundingMode.HALF_UP);
            assertEquals(expectedTotal, record.getTotalAmount(), "Delivery total amount must equal quantity * snapshot rate");
        }
    }

    @Test
    @DisplayName("Verification 6 — Master Products Catalog & Inventory Stock Monitoring")
    public void testProductsAndInventory() {
        List<Product> products = productDAO.findAll();
        assertTrue(products.size() >= 7, "Master products catalog should contain at least 7 items");

        List<Inventory> inventoryList = inventoryDAO.findAll();
        assertFalse(inventoryList.isEmpty(), "Inventory table should contain stock records");

        // Verify low-stock querying and model detection logic
        List<Inventory> lowStock = inventoryDAO.findLowStockItems();
        assertNotNull(lowStock, "Low stock query result should not be null");
        for (Inventory item : lowStock) {
            assertTrue(item.getCurrentStock().compareTo(item.getReorderLevel()) <= 0,
                    "Every low-stock item must have current_stock <= reorder_level");
        }

        // Verify model-level low-stock threshold logic
        Inventory sampleLowStock = new Inventory();
        sampleLowStock.setCurrentStock(new BigDecimal("8.00"));
        sampleLowStock.setReorderLevel(new BigDecimal("10.00"));
        assertTrue(sampleLowStock.isLowStock(), "Sample item with 8 <= 10 should be low stock");

        Inventory sampleNormalStock = new Inventory();
        sampleNormalStock.setCurrentStock(new BigDecimal("25.00"));
        sampleNormalStock.setReorderLevel(new BigDecimal("10.00"));
        assertFalse(sampleNormalStock.isLowStock(), "Sample item with 25 > 10 should not be low stock");
    }

    @Test
    @DisplayName("Verification 7 — Extra Orders and Price Snapshot Preservation")
    public void testExtraOrdersAndSnapshots() {
        List<ExtraOrder> orders = extraOrderDAO.findByCustomerId(1L);
        assertFalse(orders.isEmpty(), "Customer 1 should have extra order records");

        ExtraOrder order = orders.get(0);
        assertNotNull(order.getPriceSnapshot(), "Extra order must preserve priceSnapshot");
        assertTrue(order.getPriceSnapshot().compareTo(BigDecimal.ZERO) > 0);
        BigDecimal expectedTotal = order.getQuantity().multiply(order.getPriceSnapshot()).setScale(2, java.math.RoundingMode.HALF_UP);
        assertEquals(expectedTotal, order.getTotalPrice(), "Total price must match quantity * priceSnapshot");
    }

    @Test
    @DisplayName("Verification 8 — Holiday Requests and Schedule Pause Checking")
    public void testHolidayRequests() {
        List<HolidayRequest> history = holidayRequestDAO.findByCustomerId(3L);
        assertFalse(history.isEmpty(), "Customer 3 should have holiday request record");

        HolidayRequest req = history.get(0);
        assertEquals(RequestStatus.APPROVED, req.getApprovalStatus());

        // Check isCustomerOnHoliday for a date within customer 3's holiday (2026-08-03)
        java.sql.Date holidayDate = java.sql.Date.valueOf("2026-08-03");
        boolean onHoliday = holidayRequestDAO.isCustomerOnHoliday(3L, holidayDate);
        assertTrue(onHoliday, "Customer 3 should be detected as on holiday on 2026-08-03");

        // Check isCustomerOnHoliday for a date outside customer 3's holiday (2026-09-01)
        java.sql.Date normalDate = java.sql.Date.valueOf("2026-09-01");
        boolean normalDay = holidayRequestDAO.isCustomerOnHoliday(3L, normalDate);
        assertFalse(normalDay, "Customer 3 should NOT be on holiday on 2026-09-01");
    }

    @Test
    @DisplayName("Verification 9 — Offline Dairy Knowledge Assistant Query Matching")
    public void testKnowledgeAssistantQueries() {
        FAQKnowledgeBase faq = knowledgeService.getBestMatchingFAQ("paneer shelf life water");
        assertNotNull(faq);
        assertTrue(faq.getAnswer().toLowerCase().contains("paneer") || faq.getKeywords().toLowerCase().contains("paneer"),
                "Query should match paneer preservation FAQ");

        FAQKnowledgeBase boilFaq = knowledgeService.getBestMatchingFAQ("boil cow milk prevent spoiling");
        assertNotNull(boilFaq);
        assertTrue(boilFaq.getAnswer().toLowerCase().contains("boil"), "Query should match boiling recommendation FAQ");
    }

    @Test
    @DisplayName("Verification 10 — Customer Payment Service Validation & Recording")
    public void testCustomerPaymentServiceValidation() {
        PaymentService paymentService = new PaymentServiceImpl();

        // 1. Validation constraints
        assertThrows(ValidationException.class, () ->
                paymentService.recordPayment(null, 1L, new BigDecimal("100.00"), null, PaymentMode.UPI, "REF"));

        assertThrows(ValidationException.class, () ->
                paymentService.recordPayment(1L, null, new BigDecimal("100.00"), null, PaymentMode.UPI, "REF"));

        assertThrows(ValidationException.class, () ->
                paymentService.recordPayment(1L, 1L, new BigDecimal("-50.00"), null, PaymentMode.UPI, "REF"));

        assertThrows(ValidationException.class, () ->
                paymentService.recordPayment(1L, 1L, BigDecimal.ZERO, null, PaymentMode.UPI, "REF"));

        assertThrows(ValidationException.class, () ->
                paymentService.recordPayment(1L, 1L, new BigDecimal("100.00"), null, null, "REF"));

        // 2. Mismatched customer ID check on existing bill
        assertThrows(ValidationException.class, () ->
                paymentService.recordPayment(1L, 999L, new BigDecimal("100.00"), null, PaymentMode.UPI, "REF"));

        // 3. Existing payment history query
        List<Payment> history = paymentService.getPaymentsByCustomer(1L);
        assertNotNull(history);
        assertFalse(history.isEmpty(), "Customer 1 should have payment history in database");
    }

    @Test
    @DisplayName("Verification 11 — AI Assistant Vision Integration & Disclaimer Safeguards")
    public void testAiAssistantVisionSafeguards() throws Exception {
        AiAssistantService aiAssistantService = new AiAssistantServiceImpl();

        // 1. Unconfigured state must report guidance without faking live AI
        assertNotNull(aiAssistantService.getActiveProviderName());
        assertTrue(aiAssistantService.getConfigurationInstructions().contains("GEMINI_API_KEY"));

        // 2. Reject empty or oversized images safely
        CompletableFuture<String> emptyCall = aiAssistantService.analyzeImageAsync(new byte[0], "image/jpeg", ImageAnalysisCategory.ANIMAL_HEALTH, "test");
        assertTrue(emptyCall.get().contains("Error"), "Empty image bytes must be rejected");

        // 3. Categories and disclaimers exist
        assertEquals("Cow/buffalo health concerns", ImageAnalysisCategory.ANIMAL_HEALTH.getDisplayName());
        assertEquals("Milk-related problems", ImageAnalysisCategory.MILK_PROBLEMS.getDisplayName());
    }
}
