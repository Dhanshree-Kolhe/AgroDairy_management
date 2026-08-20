package com.agrodairy.management.dao;

import com.agrodairy.management.model.Product;
import com.agrodairy.management.model.enums.ProductCategory;

import java.util.List;
import java.util.Optional;

public interface ProductDAO {
    Long insert(Product product);
    boolean update(Product product);
    boolean delete(Long productId);
    Optional<Product> findById(Long productId);
    Optional<Product> findByProductCode(String productCode);
    List<Product> findAll();
    List<Product> findByCategory(ProductCategory category);
    List<Product> findActiveProducts();
}
