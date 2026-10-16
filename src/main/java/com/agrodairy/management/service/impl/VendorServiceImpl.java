package com.agrodairy.management.service.impl;

import com.agrodairy.management.dao.VendorDAO;
import com.agrodairy.management.dao.impl.VendorDAOImpl;
import com.agrodairy.management.exception.ResourceNotFoundException;
import com.agrodairy.management.exception.ValidationException;
import com.agrodairy.management.model.Vendor;
import com.agrodairy.management.service.VendorService;

import java.util.List;

public class VendorServiceImpl implements VendorService {

    private final VendorDAO vendorDAO;

    public VendorServiceImpl() {
        this.vendorDAO = new VendorDAOImpl();
    }

    public VendorServiceImpl(VendorDAO vendorDAO) {
        this.vendorDAO = vendorDAO;
    }

    @Override
    public Vendor addVendor(Vendor vendor) {
        validateVendor(vendor);

        if (vendor.getVendorCode() == null || vendor.getVendorCode().trim().isEmpty()) {
            vendor.setVendorCode("VEND-" + (System.currentTimeMillis() % 100000));
        } else if (vendorDAO.findByVendorCode(vendor.getVendorCode().trim()).isPresent()) {
            throw new ValidationException("Vendor code already exists: " + vendor.getVendorCode());
        }

        Long id = vendorDAO.insert(vendor);
        vendor.setVendorId(id);
        return vendor;
    }

    @Override
    public Vendor updateVendor(Vendor vendor) {
        if (vendor == null || vendor.getVendorId() == null) {
            throw new ValidationException("Vendor ID is required for update.");
        }
        getVendorById(vendor.getVendorId()); // verify exists
        validateVendor(vendor);

        boolean updated = vendorDAO.update(vendor);
        if (!updated) {
            throw new ValidationException("Failed to update vendor with ID: " + vendor.getVendorId());
        }
        return vendor;
    }

    @Override
    public boolean deleteVendor(Long vendorId) {
        getVendorById(vendorId); // verify exists
        return vendorDAO.delete(vendorId);
    }

    @Override
    public Vendor getVendorById(Long vendorId) {
        if (vendorId == null || vendorId <= 0) {
            throw new ValidationException("Invalid vendor ID.");
        }
        return vendorDAO.findById(vendorId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found with ID: " + vendorId));
    }

    @Override
    public Vendor getVendorByUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new ValidationException("Invalid user ID.");
        }
        return vendorDAO.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found for user ID: " + userId));
    }

    @Override
    public Vendor getVendorByCode(String vendorCode) {
        if (vendorCode == null || vendorCode.trim().isEmpty()) {
            throw new ValidationException("Vendor code cannot be empty.");
        }
        return vendorDAO.findByVendorCode(vendorCode.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found with code: " + vendorCode));
    }

    @Override
    public List<Vendor> getAllVendors() {
        return vendorDAO.findAll();
    }

    @Override
    public void validateVendor(Vendor vendor) {
        if (vendor == null) {
            throw new ValidationException("Vendor cannot be null.");
        }
        if (vendor.getBusinessName() == null || vendor.getBusinessName().trim().isEmpty()) {
            throw new ValidationException("Business name is required.");
        }
        if (vendor.getOwnerName() == null || vendor.getOwnerName().trim().isEmpty()) {
            throw new ValidationException("Owner name is required.");
        }
        if (vendor.getPhone() == null || !vendor.getPhone().matches("^[0-9]{10}$")) {
            throw new ValidationException("Phone number must be exactly 10 digits.");
        }
        if (vendor.getEmail() != null && !vendor.getEmail().trim().isEmpty()) {
            if (!vendor.getEmail().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                throw new ValidationException("Invalid email format.");
            }
        }
        if (vendor.getGstNumber() != null && !vendor.getGstNumber().trim().isEmpty()) {
            if (vendor.getGstNumber().trim().length() != 15) {
                throw new ValidationException("GST number must be 15 alphanumeric characters.");
            }
        }
    }
}
