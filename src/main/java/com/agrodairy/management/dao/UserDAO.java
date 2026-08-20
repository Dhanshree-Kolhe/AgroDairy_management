package com.agrodairy.management.dao;

import com.agrodairy.management.model.User;
import java.util.List;
import java.util.Optional;

public interface UserDAO {
    Long insert(User user);
    boolean update(User user);
    boolean delete(Long userId);
    Optional<User> findById(Long userId);
    Optional<User> findByUsername(String username);
    List<User> findAll();
}
