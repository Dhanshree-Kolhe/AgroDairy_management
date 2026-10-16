package com.agrodairy.management.service;

import com.agrodairy.management.model.Product;
import com.agrodairy.management.model.enums.ProductCategory;

import java.util.List;

/**
 * Service interface for Dairy Product catalog operations and business logic.
 */
public interface ProductService {
    Product addProduct(Product product);
    Product updateProduct(Product product);
    boolean deleteProduct(Long productId);
    Product getProductById(Long productId);
    Product getProductByCode(String productCode);
    List<Product> getAllProducts();
    List<Product> getActiveProducts();
    List<Product> getProductsByCategory(ProductCategory category);
    void validateProduct(Product product);
}
