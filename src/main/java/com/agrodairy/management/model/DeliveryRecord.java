package com.agrodairy.management.model;

import com.agrodairy.management.model.enums.DeliveryShift;
import com.agrodairy.management.model.enums.DeliveryStatus;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.Objects;

/**
 * Domain entity representing daily delivery shift logs with immutable price snapshotting.
 */
public class DeliveryRecord {
    private Long deliveryId;
    private Long customerId;
    private Date deliveryDate;
    private DeliveryShift deliveryShift;
    private BigDecimal deliveredQuantity;
    private BigDecimal ratePerLiter; // PRICE SNAPSHOT AT DELIVERY TIME
    private BigDecimal totalAmount;   // deliveredQuantity * ratePerLiter
    private DeliveryStatus status;
    private String remarks;
    private Timestamp createdAt;

    // Transient reference for UI
    private Customer customer;

    public DeliveryRecord() {}

    public DeliveryRecord(Long deliveryId, Long customerId, Date deliveryDate, DeliveryShift deliveryShift,
                          BigDecimal deliveredQuantity, BigDecimal ratePerLiter, BigDecimal totalAmount,
                          DeliveryStatus status, String remarks) {
        this.deliveryId = deliveryId;
        this.customerId = customerId;
        this.deliveryDate = deliveryDate;
        this.deliveryShift = deliveryShift;
        this.deliveredQuantity = deliveredQuantity;
        this.ratePerLiter = ratePerLiter;
        this.totalAmount = totalAmount;
        this.status = status;
        this.remarks = remarks;
    }

    public Long getDeliveryId() { return deliveryId; }
    public void setDeliveryId(Long deliveryId) { this.deliveryId = deliveryId; }

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }

    public Date getDeliveryDate() { return deliveryDate; }
    public void setDeliveryDate(Date deliveryDate) { this.deliveryDate = deliveryDate; }

    public DeliveryShift getDeliveryShift() { return deliveryShift; }
    public void setDeliveryShift(DeliveryShift deliveryShift) { this.deliveryShift = deliveryShift; }

    public BigDecimal getDeliveredQuantity() { return deliveredQuantity; }
    public void setDeliveredQuantity(BigDecimal deliveredQuantity) { this.deliveredQuantity = deliveredQuantity; }

    public BigDecimal getRatePerLiter() { return ratePerLiter; }
    public void setRatePerLiter(BigDecimal ratePerLiter) { this.ratePerLiter = ratePerLiter; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public DeliveryStatus getStatus() { return status; }
    public void setStatus(DeliveryStatus status) { this.status = status; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DeliveryRecord that = (DeliveryRecord) o;
        return Objects.equals(deliveryId, that.deliveryId) && Objects.equals(customerId, that.customerId) && Objects.equals(deliveryDate, that.deliveryDate) && deliveryShift == that.deliveryShift;
    }

    @Override
    public int hashCode() {
        return Objects.hash(deliveryId, customerId, deliveryDate, deliveryShift);
    }

    @Override
    public String toString() {
        return "DeliveryRecord{" +
                "deliveryId=" + deliveryId +
                ", customerId=" + customerId +
                ", deliveryDate=" + deliveryDate +
                ", deliveryShift=" + deliveryShift +
                ", deliveredQuantity=" + deliveredQuantity +
                ", ratePerLiter=" + ratePerLiter +
                ", totalAmount=" + totalAmount +
                ", status=" + status +
                '}';
    }
}
