package com.agrodairy.management.model;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Objects;

/**
 * Domain entity representing Inventory Stock Tracking.
 */
public class Inventory {
    private Long inventoryId;
    private Long productId;
    private BigDecimal openingStock;
    private BigDecimal currentStock;
    private BigDecimal minimumStock;
    private BigDecimal reorderLevel;
    private Timestamp lastUpdated;

    // Transient reference object for UI binding
    private Product product;

    public Inventory() {}

    public Inventory(Long inventoryId, Long productId, BigDecimal openingStock, BigDecimal currentStock, BigDecimal minimumStock, BigDecimal reorderLevel) {
        this.inventoryId = inventoryId;
        this.productId = productId;
        this.openingStock = openingStock;
        this.currentStock = currentStock;
        this.minimumStock = minimumStock;
        this.reorderLevel = reorderLevel;
    }

    public Long getInventoryId() { return inventoryId; }
    public void setInventoryId(Long inventoryId) { this.inventoryId = inventoryId; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public BigDecimal getOpeningStock() { return openingStock; }
    public void setOpeningStock(BigDecimal openingStock) { this.openingStock = openingStock; }

    public BigDecimal getCurrentStock() { return currentStock; }
    public void setCurrentStock(BigDecimal currentStock) { this.currentStock = currentStock; }

    public BigDecimal getMinimumStock() { return minimumStock; }
    public void setMinimumStock(BigDecimal minimumStock) { this.minimumStock = minimumStock; }

    public BigDecimal getReorderLevel() { return reorderLevel; }
    public void setReorderLevel(BigDecimal reorderLevel) { this.reorderLevel = reorderLevel; }

    public Timestamp getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(Timestamp lastUpdated) { this.lastUpdated = lastUpdated; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public boolean isLowStock() {
        return currentStock != null && reorderLevel != null && currentStock.compareTo(reorderLevel) <= 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Inventory inventory = (Inventory) o;
        return Objects.equals(inventoryId, inventory.inventoryId) && Objects.equals(productId, inventory.productId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(inventoryId, productId);
    }

    @Override
    public String toString() {
        return "Inventory{" +
                "inventoryId=" + inventoryId +
                ", productId=" + productId +
                ", currentStock=" + currentStock +
                ", reorderLevel=" + reorderLevel +
                '}';
    }
}
