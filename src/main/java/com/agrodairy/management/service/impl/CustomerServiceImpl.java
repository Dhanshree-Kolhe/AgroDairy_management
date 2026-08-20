package com.agrodairy.management.service.impl;

import com.agrodairy.management.dao.CustomerDAO;
import com.agrodairy.management.dao.impl.CustomerDAOImpl;
import com.agrodairy.management.exception.ResourceNotFoundException;
import com.agrodairy.management.exception.ValidationException;
import com.agrodairy.management.model.Customer;
import com.agrodairy.management.model.enums.SubscriptionStatus;
import com.agrodairy.management.service.CustomerService;

import java.math.BigDecimal;
import java.util.List;

public class CustomerServiceImpl implements CustomerService {
    private final CustomerDAO customerDAO;

    public CustomerServiceImpl() {
        this.customerDAO = new CustomerDAOImpl();
    }

    public CustomerServiceImpl(CustomerDAO customerDAO) {
        this.customerDAO = customerDAO;
    }

    @Override
    public Customer addCustomer(Customer customer) {
        validateCustomerInfo(customer);
        if (customer.getCustomerCode() == null || customer.getCustomerCode().trim().isEmpty()) {
            customer.setCustomerCode("CUST-" + System.currentTimeMillis() % 100000);
        } else if (customerDAO.findByCustomerCode(customer.getCustomerCode()).isPresent()) {
            throw new ValidationException("Customer code already exists: " + customer.getCustomerCode());
        }

        Long id = customerDAO.insert(customer);
        customer.setCustomerId(id);
        return customer;
    }

    @Override
    public Customer updateCustomer(Customer customer) {
        if (customer == null || customer.getCustomerId() == null) {
            throw new ValidationException("Customer ID is required for update.");
        }
        getCustomerById(customer.getCustomerId()); // Ensure exists
        validateCustomerInfo(customer);

        boolean updated = customerDAO.update(customer);
        if (!updated) {
            throw new ValidationException("Failed to update customer with ID: " + customer.getCustomerId());
        }
        return customer;
    }

    @Override
    public boolean deleteCustomer(Long customerId) {
        getCustomerById(customerId);
        return customerDAO.softDelete(customerId);
    }

    @Override
    public Customer getCustomerById(Long customerId) {
        if (customerId == null || customerId <= 0) {
            throw new ValidationException("Invalid customer ID.");
        }
        return customerDAO.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with ID: " + customerId));
    }

    @Override
    public Customer getCustomerByCode(String customerCode) {
        if (customerCode == null || customerCode.trim().isEmpty()) {
            throw new ValidationException("Customer code cannot be empty.");
        }
        return customerDAO.findByCustomerCode(customerCode.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with code: " + customerCode));
    }

    @Override
    public List<Customer> getCustomersByVendor(Long vendorId) {
        if (vendorId == null || vendorId <= 0) {
            throw new ValidationException("Invalid vendor ID.");
        }
        return customerDAO.findByVendorId(vendorId);
    }

    @Override
    public List<Customer> getAllCustomers() {
        return customerDAO.findAll();
    }

    @Override
    public List<Customer> searchCustomers(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllCustomers();
        }
        return customerDAO.search(query.trim());
    }

    @Override
    public boolean updateSubscriptionStatus(Long customerId, SubscriptionStatus status) {
        getCustomerById(customerId);
        if (status == null) throw new ValidationException("Status cannot be null.");
        return customerDAO.updateStatus(customerId, status);
    }

    @Override
    public void validateCustomerInfo(Customer c) {
        if (c == null) throw new ValidationException("Customer cannot be null.");
        if (c.getFullName() == null || c.getFullName().trim().isEmpty()) {
            throw new ValidationException("Customer name is required.");
        }
        validateMobileNumber(c.getPhone());
        if (c.getVendorId() == null || c.getVendorId() <= 0) {
            throw new ValidationException("Vendor ID is required.");
        }
        if (c.getArea() == null || c.getArea().trim().isEmpty()) {
            throw new ValidationException("Area/Zone is required.");
        }
        if (c.getMilkType() == null) {
            throw new ValidationException("Milk type is required.");
        }
        if (c.getDeliveryShift() == null) {
            throw new ValidationException("Delivery shift is required.");
        }
        validateQuantities(c.getMorningQuantity(), c.getEveningQuantity());
        validateMilkRate(c.getCurrentPricePerLiter());
    }

    @Override
    public void validateMobileNumber(String mobile) {
        if (mobile == null || !mobile.matches("^[0-9]{10}$")) {
            throw new ValidationException("Invalid mobile number format. Must be exactly 10 digits.");
        }
    }

    @Override
    public void validateQuantities(BigDecimal morningQty, BigDecimal eveningQty) {
        if (morningQty == null || morningQty.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("Morning quantity cannot be negative.");
        }
        if (eveningQty == null || eveningQty.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("Evening quantity cannot be negative.");
        }
        if (morningQty.compareTo(BigDecimal.ZERO) == 0 && eveningQty.compareTo(BigDecimal.ZERO) == 0) {
            throw new ValidationException("Both morning and evening delivery quantities cannot be zero.");
        }
    }

    @Override
    public void validateMilkRate(BigDecimal rate) {
        if (rate == null || rate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Milk price per liter must be greater than zero.");
        }
    }
}
