package com.agrodairy.management.dao;

import com.agrodairy.management.model.Customer;
import com.agrodairy.management.model.enums.SubscriptionStatus;

import java.util.List;
import java.util.Optional;

public interface CustomerDAO {
    Long insert(Customer customer);
    boolean update(Customer customer);
    boolean updateStatus(Long customerId, SubscriptionStatus status);
    boolean softDelete(Long customerId);
    Optional<Customer> findById(Long customerId);
    Optional<Customer> findByCustomerCode(String customerCode);
    Optional<Customer> findByUserId(Long userId);
    List<Customer> findAll();
    List<Customer> findByVendorId(Long vendorId);
    List<Customer> findByArea(String area);
    List<Customer> search(String query);
}
