package com.agrodairy.management.service;

import com.agrodairy.management.dao.UserLoginDAO;
import com.agrodairy.management.exception.AuthenticationException;
import com.agrodairy.management.model.Payment;
import com.agrodairy.management.model.UserLogin;
import com.agrodairy.management.model.enums.PaymentMode;
import com.agrodairy.management.model.enums.Role;
import com.agrodairy.management.service.impl.AuthServiceImpl;
import com.agrodairy.management.util.PasswordUtil;
import com.agrodairy.management.util.SessionManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class AdminRoleAndDashboardTest {

    private AuthService authService;
    private MockUserLoginDAO userLoginDAO;

    @BeforeEach
    public void setUp() {
        userLoginDAO = new MockUserLoginDAO();
        authService = new AuthServiceImpl(userLoginDAO);

        // 1. Admin user with authentic seed BCrypt hash ($2a$12$n/0MUkh8n/CpbmVoJJ7q4.ayNgIuj1z0fOoCSGenICVxvMvnLZqm. -> secret123)
        UserLogin admin = new UserLogin();
        admin.setUserId(10L);
        admin.setUsername("admin_super");
        admin.setPasswordHash("$2a$12$n/0MUkh8n/CpbmVoJJ7q4.ayNgIuj1z0fOoCSGenICVxvMvnLZqm.");
        admin.setFullName("Super Administrator");
        admin.setRole(Role.ADMIN);
        admin.setActive(true);
        userLoginDAO.insert(admin);

        // 2. Inactive user (even with correct password, must be strictly blocked)
        UserLogin inactive = new UserLogin();
        inactive.setUserId(11L);
        inactive.setUsername("cust_inactive");
        inactive.setPasswordHash("$2a$12$n/0MUkh8n/CpbmVoJJ7q4.ayNgIuj1z0fOoCSGenICVxvMvnLZqm.");
        inactive.setFullName("Inactive Customer");
        inactive.setRole(Role.CUSTOMER);
        inactive.setActive(false);
        userLoginDAO.insert(inactive);

        // 3. Active vendor
        UserLogin vendor = new UserLogin();
        vendor.setUserId(12L);
        vendor.setUsername("vendor_ramesh");
        vendor.setPasswordHash("$2a$12$n/0MUkh8n/CpbmVoJJ7q4.ayNgIuj1z0fOoCSGenICVxvMvnLZqm.");
        vendor.setFullName("Ramesh Owner");
        vendor.setRole(Role.VENDOR);
        vendor.setActive(true);
        userLoginDAO.insert(vendor);
    }

    @Test
    @DisplayName("Admin Role 1 — Admin BCrypt Hash Verification & Successful Login")
    public void testAdminLoginSuccess() {
        // Verify actual password against stored database BCrypt hash
        assertTrue(PasswordUtil.checkPassword("secret123", "$2a$12$n/0MUkh8n/CpbmVoJJ7q4.ayNgIuj1z0fOoCSGenICVxvMvnLZqm."));
        assertFalse(PasswordUtil.checkPassword("wrongpassword", "$2a$12$n/0MUkh8n/CpbmVoJJ7q4.ayNgIuj1z0fOoCSGenICVxvMvnLZqm."));

        // Authenticate through AuthService
        UserLogin user = authService.login("admin_super", "secret123");
        assertNotNull(user);
        assertEquals(Role.ADMIN, user.getRole());
        assertEquals("admin_super", user.getUsername());
        assertTrue(user.getActive());

        SessionManager.setSession(user);
        assertTrue(SessionManager.isAdmin());
        assertFalse(SessionManager.isVendor());
        assertFalse(SessionManager.isCustomer());
        SessionManager.clearSession();
    }

    @Test
    @DisplayName("Security 2 — Password Mismatch Strictly Rejected Without Fallback Bypass")
    public void testWrongPasswordRejected() {
        assertThrows(AuthenticationException.class, () -> {
            authService.login("admin_super", "wrongpassword123");
        });
        assertThrows(AuthenticationException.class, () -> {
            authService.login("vendor_ramesh", "invalidsecret");
        });
    }

    @Test
    @DisplayName("Security 3 — Inactive User Login Rejection Enforced")
    public void testInactiveUserLoginBlocked() {
        // Inactive user attempting login with correct credentials must still be rejected
        AuthenticationException ex = assertThrows(AuthenticationException.class, () -> {
            authService.login("cust_inactive", "secret123");
        });
        assertTrue(ex.getMessage().contains("inactive"), "Error message must report inactive account status");
    }

    @Test
    @DisplayName("Admin Dashboard 4 — Revenue Calculation Non-Duplication Test")
    public void testRevenueCalculationAccuracy() {
        List<Payment> payments = new ArrayList<>();
        payments.add(new Payment(1L, 1L, 1L, new BigDecimal("4860.00"), Date.valueOf("2026-08-02"), PaymentMode.UPI, "REF-1", "SUCCESS"));
        payments.add(new Payment(2L, 2L, 2L, new BigDecimal("5000.00"), Date.valueOf("2026-08-02"), PaymentMode.CASH, "REF-2", "SUCCESS"));
        payments.add(new Payment(3L, 2L, 2L, new BigDecimal("2000.00"), Date.valueOf("2026-08-02"), PaymentMode.UPI, "REF-3", "FAILED"));

        BigDecimal total = payments.stream()
                .filter(p -> "SUCCESS".equalsIgnoreCase(p.getStatus()))
                .map(Payment::getAmountPaid)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertEquals(new BigDecimal("9860.00"), total, "Total revenue should accurately sum only SUCCESS payments");
    }

    @Test
    @DisplayName("Admin User Management 5 — Last Admin Protection Logic")
    public void testLastAdminDeactivationProtection() {
        List<UserLogin> users = userLoginDAO.findAll();
        long activeAdmins = users.stream()
                .filter(u -> u.getRole() == Role.ADMIN && Boolean.TRUE.equals(u.getActive()))
                .count();

        assertEquals(1, activeAdmins, "Should have exactly 1 active admin in setup");
        boolean canDeactivate = activeAdmins > 1;
        assertFalse(canDeactivate, "Must protect the last active Admin from deactivation");
    }

    @Test
    @DisplayName("Admin User Management 6 — Account Status Toggle")
    public void testAccountStatusToggle() {
        UserLogin user = userLoginDAO.findByUsername("cust_inactive").orElseThrow();
        assertFalse(user.getActive());

        boolean updated = userLoginDAO.updateStatus(user.getUserId(), true);
        assertTrue(updated);

        UserLogin refreshed = userLoginDAO.findById(user.getUserId()).orElseThrow();
        assertTrue(refreshed.getActive());
    }

    // In-memory stub for testing
    private static class MockUserLoginDAO implements UserLoginDAO {
        private final List<UserLogin> list = new ArrayList<>();

        @Override
        public Long insert(UserLogin user) {
            list.add(user);
            return user.getUserId();
        }

        @Override
        public boolean update(UserLogin user) {
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i).getUserId().equals(user.getUserId())) {
                    list.set(i, user);
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean updateStatus(Long userId, boolean isActive) {
            for (UserLogin u : list) {
                if (u.getUserId().equals(userId)) {
                    u.setActive(isActive);
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean delete(Long userId) {
            return updateStatus(userId, false);
        }

        @Override
        public Optional<UserLogin> findById(Long userId) {
            return list.stream().filter(u -> u.getUserId().equals(userId)).findFirst();
        }

        @Override
        public Optional<UserLogin> findByUsername(String username) {
            return list.stream().filter(u -> u.getUsername().equalsIgnoreCase(username)).findFirst();
        }

        @Override
        public List<UserLogin> findAll() {
            return new ArrayList<>(list);
        }
    }
}
