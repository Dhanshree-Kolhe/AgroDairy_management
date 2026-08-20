package com.agrodairy.management.model;

import com.agrodairy.management.model.enums.ProductCategory;
import com.agrodairy.management.model.enums.ProductUnit;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Objects;

/**
 * Domain entity representing Dairy Products in master catalog with standardized ProductUnit.
 */
public class Product {
    private Long productId;
    private String productCode;
    private String productName;
    private ProductCategory category;
    private ProductUnit unit;
    private BigDecimal price;
    private String status; // ACTIVE, INACTIVE
    private Timestamp createdAt;

    public Product() {}

    public Product(Long productId, String productCode, String productName, ProductCategory category, ProductUnit unit, BigDecimal price, String status) {
        this.productId = productId;
        this.productCode = productCode;
        this.productName = productName;
        this.category = category;
        this.unit = unit;
        this.price = price;
        this.status = status;
    }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public String getProductCode() { return productCode; }
    public void setProductCode(String productCode) { this.productCode = productCode; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public ProductCategory getCategory() { return category; }
    public void setCategory(ProductCategory category) { this.category = category; }

    public ProductUnit getUnit() { return unit; }
    public void setUnit(ProductUnit unit) { this.unit = unit; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Product product = (Product) o;
        return Objects.equals(productId, product.productId) && Objects.equals(productCode, product.productCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId, productCode);
    }

    @Override
    public String toString() {
        return "Product{" +
                "productId=" + productId +
                ", productCode='" + productCode + '\'' +
                ", productName='" + productName + '\'' +
                ", category=" + category +
                ", unit=" + unit +
                ", price=" + price +
                '}';
    }
}
