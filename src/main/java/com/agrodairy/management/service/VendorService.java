package com.agrodairy.management.service;

import com.agrodairy.management.model.Vendor;

import java.util.List;

/**
 * Service interface for Vendor management and validations.
 */
public interface VendorService {
    Vendor addVendor(Vendor vendor);
    Vendor updateVendor(Vendor vendor);
    boolean deleteVendor(Long vendorId);
    Vendor getVendorById(Long vendorId);
    Vendor getVendorByUserId(Long userId);
    Vendor getVendorByCode(String vendorCode);
    List<Vendor> getAllVendors();
    void validateVendor(Vendor vendor);
}
