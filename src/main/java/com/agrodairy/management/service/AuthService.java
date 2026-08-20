package com.agrodairy.management.service;

import com.agrodairy.management.model.UserLogin;
import com.agrodairy.management.model.enums.Role;

public interface AuthService {
    UserLogin login(String username, String plainPassword);
    UserLogin findByUsername(String username);
    boolean verifyRole(UserLogin user, Role expectedRole);
    UserLogin registerUser(UserLogin user);
}
