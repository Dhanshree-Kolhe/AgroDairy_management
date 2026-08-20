package com.agrodairy.management.dao.impl;

import com.agrodairy.management.dao.UserDAO;
import com.agrodairy.management.dao.UserLoginDAO;
import com.agrodairy.management.model.User;
import com.agrodairy.management.model.UserLogin;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class UserDAOImpl implements UserDAO {
    private final UserLoginDAO loginDAO = new UserLoginDAOImpl();

    @Override
    public Long insert(User user) {
        return loginDAO.insert(user);
    }

    @Override
    public boolean update(User user) {
        return loginDAO.update(user);
    }

    @Override
    public boolean delete(Long userId) {
        return loginDAO.delete(userId);
    }

    @Override
    public Optional<User> findById(Long userId) {
        return loginDAO.findById(userId).map(ul -> (User) ul);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return loginDAO.findByUsername(username).map(ul -> (User) ul);
    }

    @Override
    public List<User> findAll() {
        return loginDAO.findAll().stream().map(ul -> (User) ul).collect(Collectors.toList());
    }
}
