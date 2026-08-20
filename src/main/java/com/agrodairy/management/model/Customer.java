package com.agrodairy.management.model;

import com.agrodairy.management.model.enums.DeliveryShift;
import com.agrodairy.management.model.enums.MilkType;
import com.agrodairy.management.model.enums.SubscriptionStatus;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Objects;

/**
 * Domain entity representing Customer profiles and delivery subscriptions.
 */
public class Customer {
    private Long customerId;
    private Long userId;
    private Long vendorId;
    private String customerCode;
    private String fullName;
    private String phone;
    private String houseNumber;
    private String street;
    private String area;
    private String city;
    private String pincode;
    private String address;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private MilkType milkType;
    private BigDecimal morningQuantity;
    private BigDecimal eveningQuantity;
    private DeliveryShift deliveryShift;
    private BigDecimal currentPricePerLiter;
    private SubscriptionStatus status;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public Customer() {}

    public Customer(Long customerId, Long userId, Long vendorId, String customerCode, String fullName, String phone,
                    String houseNumber, String street, String area, String city, String pincode, String address,
                    BigDecimal latitude, BigDecimal longitude, MilkType milkType, BigDecimal morningQuantity,
                    BigDecimal eveningQuantity, DeliveryShift deliveryShift, BigDecimal currentPricePerLiter,
                    SubscriptionStatus status) {
        this.customerId = customerId;
        this.userId = userId;
        this.vendorId = vendorId;
        this.customerCode = customerCode;
        this.fullName = fullName;
        this.phone = phone;
        this.houseNumber = houseNumber;
        this.street = street;
        this.area = area;
        this.city = city;
        this.pincode = pincode;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.milkType = milkType;
        this.morningQuantity = morningQuantity;
        this.eveningQuantity = eveningQuantity;
        this.deliveryShift = deliveryShift;
        this.currentPricePerLiter = currentPricePerLiter;
        this.status = status;
    }

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getVendorId() { return vendorId; }
    public void setVendorId(Long vendorId) { this.vendorId = vendorId; }

    public String getCustomerCode() { return customerCode; }
    public void setCustomerCode(String customerCode) { this.customerCode = customerCode; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getHouseNumber() { return houseNumber; }
    public void setHouseNumber(String houseNumber) { this.houseNumber = houseNumber; }

    public String getStreet() { return street; }
    public void setStreet(String street) { this.street = street; }

    public String getArea() { return area; }
    public void setArea(String area) { this.area = area; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getPincode() { return pincode; }
    public void setPincode(String pincode) { this.pincode = pincode; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }

    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }

    public MilkType getMilkType() { return milkType; }
    public void setMilkType(MilkType milkType) { this.milkType = milkType; }

    public BigDecimal getMorningQuantity() { return morningQuantity; }
    public void setMorningQuantity(BigDecimal morningQuantity) { this.morningQuantity = morningQuantity; }

    public BigDecimal getEveningQuantity() { return eveningQuantity; }
    public void setEveningQuantity(BigDecimal eveningQuantity) { this.eveningQuantity = eveningQuantity; }

    public DeliveryShift getDeliveryShift() { return deliveryShift; }
    public void setDeliveryShift(DeliveryShift deliveryShift) { this.deliveryShift = deliveryShift; }

    public BigDecimal getCurrentPricePerLiter() { return currentPricePerLiter; }
    public void setCurrentPricePerLiter(BigDecimal currentPricePerLiter) { this.currentPricePerLiter = currentPricePerLiter; }

    public SubscriptionStatus getStatus() { return status; }
    public void setStatus(SubscriptionStatus status) { this.status = status; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Customer customer = (Customer) o;
        return Objects.equals(customerId, customer.customerId) && Objects.equals(customerCode, customer.customerCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(customerId, customerCode);
    }

    @Override
    public String toString() {
        return "Customer{" +
                "customerId=" + customerId +
                ", customerCode='" + customerCode + '\'' +
                ", fullName='" + fullName + '\'' +
                ", area='" + area + '\'' +
                ", milkType=" + milkType +
                ", currentPricePerLiter=" + currentPricePerLiter +
                ", status=" + status +
                '}';
    }
}
