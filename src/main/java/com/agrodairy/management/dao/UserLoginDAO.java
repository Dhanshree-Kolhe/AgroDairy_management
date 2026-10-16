package com.agrodairy.management.dao;

import com.agrodairy.management.model.UserLogin;
import java.util.List;
import java.util.Optional;

public interface UserLoginDAO {
    Long insert(UserLogin user);
    boolean update(UserLogin user);
    boolean delete(Long userId);
    Optional<UserLogin> findById(Long userId);
    Optional<UserLogin> findByUsername(String username);
    List<UserLogin> findAll();
    default boolean updateStatus(Long userId, boolean isActive) {
        return false;
    }
}
