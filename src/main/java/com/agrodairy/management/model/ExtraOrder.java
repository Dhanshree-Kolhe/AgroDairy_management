package com.agrodairy.management.model;

import com.agrodairy.management.model.enums.RequestStatus;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.Objects;

/**
 * Domain entity representing Extra Dairy Product Orders with price snapshotting.
 */
public class ExtraOrder {
    private Long orderId;
    private Long customerId;
    private Long productId;
    private BigDecimal quantity;
    private BigDecimal priceSnapshot; // PRICE SNAPSHOT AT ORDER TIME
    private BigDecimal totalPrice;    // quantity * priceSnapshot
    private Date orderDate;
    private Date deliveryDate;
    private RequestStatus status;
    private Timestamp createdAt;

    // Transient references for UI
    private Customer customer;
    private Product product;

    public ExtraOrder() {}

    public ExtraOrder(Long orderId, Long customerId, Long productId, BigDecimal quantity, BigDecimal priceSnapshot, BigDecimal totalPrice, Date orderDate, Date deliveryDate, RequestStatus status) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.productId = productId;
        this.quantity = quantity;
        this.priceSnapshot = priceSnapshot;
        this.totalPrice = totalPrice;
        this.orderDate = orderDate;
        this.deliveryDate = deliveryDate;
        this.status = status;
    }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

    public BigDecimal getPriceSnapshot() { return priceSnapshot; }
    public void setPriceSnapshot(BigDecimal priceSnapshot) { this.priceSnapshot = priceSnapshot; }

    public BigDecimal getTotalPrice() { return totalPrice; }
    public void setTotalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; }

    public Date getOrderDate() { return orderDate; }
    public void setOrderDate(Date orderDate) { this.orderDate = orderDate; }

    public Date getDeliveryDate() { return deliveryDate; }
    public void setDeliveryDate(Date deliveryDate) { this.deliveryDate = deliveryDate; }

    public RequestStatus getStatus() { return status; }
    public void setStatus(RequestStatus status) { this.status = status; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExtraOrder that = (ExtraOrder) o;
        return Objects.equals(orderId, that.orderId) && Objects.equals(customerId, that.customerId) && Objects.equals(productId, that.productId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orderId, customerId, productId);
    }

    @Override
    public String toString() {
        return "ExtraOrder{" +
                "orderId=" + orderId +
                ", customerId=" + customerId +
                ", productId=" + productId +
                ", quantity=" + quantity +
                ", priceSnapshot=" + priceSnapshot +
                ", totalPrice=" + totalPrice +
                ", status=" + status +
                '}';
    }
}
