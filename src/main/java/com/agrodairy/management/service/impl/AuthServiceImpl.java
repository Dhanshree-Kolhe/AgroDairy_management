package com.agrodairy.management.service.impl;

import com.agrodairy.management.dao.UserLoginDAO;
import com.agrodairy.management.dao.impl.UserLoginDAOImpl;
import com.agrodairy.management.exception.AuthenticationException;
import com.agrodairy.management.exception.ValidationException;
import com.agrodairy.management.model.UserLogin;
import com.agrodairy.management.model.enums.Role;
import com.agrodairy.management.service.AuthService;
import com.agrodairy.management.util.PasswordUtil;

import java.util.Optional;

public class AuthServiceImpl implements AuthService {
    private final UserLoginDAO userLoginDAO;

    public AuthServiceImpl() {
        this.userLoginDAO = new UserLoginDAOImpl();
    }

    public AuthServiceImpl(UserLoginDAO userLoginDAO) {
        this.userLoginDAO = userLoginDAO;
    }

    @Override
    public UserLogin login(String username, String plainPassword) {
        if (username == null || username.trim().isEmpty()) {
            throw new ValidationException("Username cannot be empty.");
        }
        if (plainPassword == null || plainPassword.trim().isEmpty()) {
            throw new ValidationException("Password cannot be empty.");
        }

        Optional<UserLogin> userOpt = userLoginDAO.findByUsername(username.trim());
        if (userOpt.isEmpty()) {
            throw new AuthenticationException("Invalid username or password.");
        }

        UserLogin user = userOpt.get();
        if (Boolean.FALSE.equals(user.getActive())) {
            throw new AuthenticationException("User account is inactive. Please contact system administrator.");
        }

        if (!PasswordUtil.checkPassword(plainPassword, user.getPasswordHash())) {
            throw new AuthenticationException("Invalid username or password.");
        }

        return user;
    }

    @Override
    public UserLogin findByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new ValidationException("Username cannot be empty.");
        }
        return userLoginDAO.findByUsername(username.trim())
                .orElseThrow(() -> new AuthenticationException("User not found: " + username));
    }

    @Override
    public boolean verifyRole(UserLogin user, Role expectedRole) {
        if (user == null || expectedRole == null) return false;
        return user.getRole() == expectedRole;
    }

    @Override
    public UserLogin registerUser(UserLogin user) {
        if (user == null) throw new ValidationException("User object cannot be null.");
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            throw new ValidationException("Username is required.");
        }
        if (user.getPasswordHash() == null || user.getPasswordHash().trim().isEmpty()) {
            throw new ValidationException("Password is required.");
        }
        if (user.getFullName() == null || user.getFullName().trim().isEmpty()) {
            throw new ValidationException("Full name is required.");
        }
        if (user.getRole() == null) {
            throw new ValidationException("Role is required.");
        }

        if (userLoginDAO.findByUsername(user.getUsername().trim()).isPresent()) {
            throw new ValidationException("Username already exists: " + user.getUsername());
        }

        // Hash password if not already BCrypt hashed
        if (!user.getPasswordHash().startsWith("$2a$") && !user.getPasswordHash().startsWith("$2b$")) {
            user.setPasswordHash(PasswordUtil.hashPassword(user.getPasswordHash()));
        }

        Long id = userLoginDAO.insert(user);
        user.setUserId(id);
        return user;
    }
}
