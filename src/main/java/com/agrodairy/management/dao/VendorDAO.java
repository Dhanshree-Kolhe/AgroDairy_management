package com.agrodairy.management.dao;

import com.agrodairy.management.model.Vendor;
import java.util.List;
import java.util.Optional;

public interface VendorDAO {
    Long insert(Vendor vendor);
    boolean update(Vendor vendor);
    boolean delete(Long vendorId);
    Optional<Vendor> findById(Long vendorId);
    Optional<Vendor> findByUserId(Long userId);
    Optional<Vendor> findByVendorCode(String vendorCode);
    List<Vendor> findAll();
}
