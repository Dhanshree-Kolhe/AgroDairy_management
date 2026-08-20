package com.agrodairy.management.service;

import com.agrodairy.management.dao.UserLoginDAO;
import com.agrodairy.management.exception.AuthenticationException;
import com.agrodairy.management.exception.ValidationException;
import com.agrodairy.management.model.UserLogin;
import com.agrodairy.management.model.enums.Role;
import com.agrodairy.management.service.impl.AuthServiceImpl;
import com.agrodairy.management.util.PasswordUtil;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class AuthServiceTest {

    private AuthService authService;
    private StubUserLoginDAO userLoginDAO;

    @BeforeEach
    public void setUp() {
        userLoginDAO = new StubUserLoginDAO();
        authService = new AuthServiceImpl(userLoginDAO);

        UserLogin user = new UserLogin();
        user.setUserId(1L);
        user.setUsername("vendor_ramesh");
        user.setPasswordHash(PasswordUtil.hashPassword("secret123"));
        user.setFullName("Ramesh Owner");
        user.setRole(Role.VENDOR);
        user.setActive(true);

        userLoginDAO.insert(user);
    }

    @Test
    public void test1_SuccessfulLogin() {
        UserLogin loggedIn = authService.login("vendor_ramesh", "secret123");
        assertNotNull(loggedIn);
        assertEquals("vendor_ramesh", loggedIn.getUsername());
        assertEquals(Role.VENDOR, loggedIn.getRole());
    }

    @Test
    public void test2_InvalidLogin() {
        assertThrows(AuthenticationException.class, () -> {
            authService.login("vendor_ramesh", "wrongpassword");
        });

        assertThrows(AuthenticationException.class, () -> {
            authService.login("non_existent_user", "secret123");
        });

        assertThrows(ValidationException.class, () -> {
            authService.login("", "secret123");
        });
    }

    // Lightweight In-Memory Stub for DAO Testing
    private static class StubUserLoginDAO implements UserLoginDAO {
        private final List<UserLogin> users = new ArrayList<>();
        private long idSeq = 1;

        @Override
        public Long insert(UserLogin user) {
            user.setUserId(idSeq++);
            users.add(user);
            return user.getUserId();
        }

        @Override
        public boolean update(UserLogin user) { return true; }

        @Override
        public boolean delete(Long userId) { return true; }

        @Override
        public Optional<UserLogin> findById(Long userId) {
            return users.stream().filter(u -> u.getUserId().equals(userId)).findFirst();
        }

        @Override
        public Optional<UserLogin> findByUsername(String username) {
            return users.stream().filter(u -> u.getUsername().equals(username)).findFirst();
        }

        @Override
        public List<UserLogin> findAll() { return users; }
    }
}
