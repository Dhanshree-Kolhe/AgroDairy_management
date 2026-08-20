package com.agrodairy.management.service;

import com.agrodairy.management.model.Customer;
import com.agrodairy.management.model.enums.SubscriptionStatus;

import java.math.BigDecimal;
import java.util.List;

public interface CustomerService {
    Customer addCustomer(Customer customer);
    Customer updateCustomer(Customer customer);
    boolean deleteCustomer(Long customerId);
    Customer getCustomerById(Long customerId);
    Customer getCustomerByCode(String customerCode);
    List<Customer> getCustomersByVendor(Long vendorId);
    List<Customer> getAllCustomers();
    List<Customer> searchCustomers(String query);
    boolean updateSubscriptionStatus(Long customerId, SubscriptionStatus status);
    
    // Business Validation Helpers
    void validateCustomerInfo(Customer customer);
    void validateMobileNumber(String mobile);
    void validateQuantities(BigDecimal morningQty, BigDecimal eveningQty);
    void validateMilkRate(BigDecimal rate);
}
