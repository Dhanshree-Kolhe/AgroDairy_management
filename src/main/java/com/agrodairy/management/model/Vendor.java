package com.agrodairy.management.model;

import java.sql.Timestamp;
import java.util.Objects;

/**
 * Domain entity representing Vendor (Admin) profiles.
 */
public class Vendor {
    private Long vendorId;
    private Long userId;
    private String vendorCode;
    private String businessName;
    private String ownerName;
    private String phone;
    private String email;
    private String address;
    private String gstNumber;
    private Timestamp createdAt;

    public Vendor() {}

    public Vendor(Long vendorId, Long userId, String vendorCode, String businessName, String ownerName, String phone, String email, String address, String gstNumber) {
        this.vendorId = vendorId;
        this.userId = userId;
        this.vendorCode = vendorCode;
        this.businessName = businessName;
        this.ownerName = ownerName;
        this.phone = phone;
        this.email = email;
        this.address = address;
        this.gstNumber = gstNumber;
    }

    public Long getVendorId() { return vendorId; }
    public void setVendorId(Long vendorId) { this.vendorId = vendorId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getVendorCode() { return vendorCode; }
    public void setVendorCode(String vendorCode) { this.vendorCode = vendorCode; }

    public String getBusinessName() { return businessName; }
    public void setBusinessName(String businessName) { this.businessName = businessName; }

    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getGstNumber() { return gstNumber; }
    public void setGstNumber(String gstNumber) { this.gstNumber = gstNumber; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Vendor vendor = (Vendor) o;
        return Objects.equals(vendorId, vendor.vendorId) && Objects.equals(vendorCode, vendor.vendorCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(vendorId, vendorCode);
    }

    @Override
    public String toString() {
        return "Vendor{" +
                "vendorId=" + vendorId +
                ", vendorCode='" + vendorCode + '\'' +
                ", businessName='" + businessName + '\'' +
                ", ownerName='" + ownerName + '\'' +
                ", phone='" + phone + '\'' +
                '}';
    }
}
