package com.agrodairy.management.model;

import com.agrodairy.management.model.enums.Role;
import java.sql.Timestamp;

/**
 * Legacy/Compatible User entity extending UserLogin.
 */
public class User extends UserLogin {

    public User() {
        super();
    }

    public User(Long userId, String username, String passwordHash, String fullName, String email, String phone, Role role, Boolean active) {
        super(userId, username, passwordHash, fullName, email, phone, role, active);
    }
}
