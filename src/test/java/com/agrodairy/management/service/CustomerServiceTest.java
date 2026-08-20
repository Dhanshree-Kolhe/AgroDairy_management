package com.agrodairy.management.service;

import com.agrodairy.management.dao.CustomerDAO;
import com.agrodairy.management.exception.ValidationException;
import com.agrodairy.management.model.Customer;
import com.agrodairy.management.model.enums.DeliveryShift;
import com.agrodairy.management.model.enums.MilkType;
import com.agrodairy.management.model.enums.SubscriptionStatus;
import com.agrodairy.management.service.impl.CustomerServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class CustomerServiceTest {

    private CustomerService customerService;
    private StubCustomerDAO customerDAO;

    @BeforeEach
    public void setUp() {
        customerDAO = new StubCustomerDAO();
        customerService = new CustomerServiceImpl(customerDAO);
    }

    @Test
    public void test3_CustomerCreationValidation() {
        Customer c = new Customer();
        c.setVendorId(1L);
        c.setCustomerCode("CUST-9901");
        c.setFullName("Rahul Verma");
        c.setPhone("9876500001");
        c.setArea("Dharampeth");
        c.setCity("Nagpur");
        c.setPincode("440010");
        c.setMilkType(MilkType.COW);
        c.setMorningQuantity(new BigDecimal("1.5"));
        c.setEveningQuantity(new BigDecimal("1.0"));
        c.setDeliveryShift(DeliveryShift.BOTH);
        c.setCurrentPricePerLiter(new BigDecimal("60.00"));
        c.setStatus(SubscriptionStatus.ACTIVE);

        Customer saved = customerService.addCustomer(c);
        assertNotNull(saved.getCustomerId());
        assertEquals("Rahul Verma", saved.getFullName());

        // Validate Invalid Phone Number
        Customer invalidPhone = new Customer();
        invalidPhone.setPhone("123");
        assertThrows(ValidationException.class, () -> customerService.validateMobileNumber(invalidPhone.getPhone()));

        // Validate Negative Rate
        assertThrows(ValidationException.class, () -> customerService.validateMilkRate(new BigDecimal("-10.00")));
    }

    private static class StubCustomerDAO implements CustomerDAO {
        private final List<Customer> customers = new ArrayList<>();
        private long seq = 1;

        @Override public Long insert(Customer customer) { customer.setCustomerId(seq++); customers.add(customer); return customer.getCustomerId(); }
        @Override public boolean update(Customer customer) { return true; }
        @Override public boolean updateStatus(Long customerId, SubscriptionStatus status) { return true; }
        @Override public boolean softDelete(Long customerId) { return true; }
        @Override public Optional<Customer> findById(Long customerId) { return customers.stream().filter(c -> c.getCustomerId().equals(customerId)).findFirst(); }
        @Override public Optional<Customer> findByCustomerCode(String customerCode) { return customers.stream().filter(c -> c.getCustomerCode().equals(customerCode)).findFirst(); }
        @Override public Optional<Customer> findByUserId(Long userId) { return Optional.empty(); }
        @Override public List<Customer> findAll() { return customers; }
        @Override public List<Customer> findByVendorId(Long vendorId) { return customers; }
        @Override public List<Customer> findByArea(String area) { return customers; }
        @Override public List<Customer> search(String query) { return customers; }
    }
}
