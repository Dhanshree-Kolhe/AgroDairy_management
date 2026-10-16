package com.agrodairy.management.service.impl;

import com.agrodairy.management.dao.InventoryDAO;
import com.agrodairy.management.dao.ProductDAO;
import com.agrodairy.management.dao.impl.InventoryDAOImpl;
import com.agrodairy.management.dao.impl.ProductDAOImpl;
import com.agrodairy.management.exception.ResourceNotFoundException;
import com.agrodairy.management.exception.ValidationException;
import com.agrodairy.management.model.Inventory;
import com.agrodairy.management.model.Product;
import com.agrodairy.management.model.enums.ProductCategory;
import com.agrodairy.management.service.ProductService;

import java.math.BigDecimal;
import java.util.List;

public class ProductServiceImpl implements ProductService {

    private final ProductDAO productDAO;
    private final InventoryDAO inventoryDAO;

    public ProductServiceImpl() {
        this.productDAO = new ProductDAOImpl();
        this.inventoryDAO = new InventoryDAOImpl();
    }

    public ProductServiceImpl(ProductDAO productDAO, InventoryDAO inventoryDAO) {
        this.productDAO = productDAO;
        this.inventoryDAO = inventoryDAO;
    }

    @Override
    public Product addProduct(Product product) {
        validateProduct(product);

        if (product.getProductCode() == null || product.getProductCode().trim().isEmpty()) {
            product.setProductCode("PROD-" + (System.currentTimeMillis() % 100000));
        } else if (productDAO.findByProductCode(product.getProductCode().trim()).isPresent()) {
            throw new ValidationException("Product code already exists: " + product.getProductCode());
        }

        Long id = productDAO.insert(product);
        product.setProductId(id);

        // Ensure baseline inventory record is initialized
        if (inventoryDAO.findByProductId(id).isEmpty()) {
            Inventory inv = new Inventory();
            inv.setProductId(id);
            inv.setOpeningStock(BigDecimal.ZERO);
            inv.setCurrentStock(BigDecimal.ZERO);
            inv.setMinimumStock(new BigDecimal("10.00"));
            inv.setReorderLevel(new BigDecimal("15.00"));
            inventoryDAO.insert(inv);
        }

        return product;
    }

    @Override
    public Product updateProduct(Product product) {
        if (product == null || product.getProductId() == null) {
            throw new ValidationException("Product ID is required for update.");
        }
        getProductById(product.getProductId()); // verify exists
        validateProduct(product);

        boolean updated = productDAO.update(product);
        if (!updated) {
            throw new ValidationException("Failed to update product with ID: " + product.getProductId());
        }
        return product;
    }

    @Override
    public boolean deleteProduct(Long productId) {
        getProductById(productId); // verify exists
        return productDAO.delete(productId);
    }

    @Override
    public Product getProductById(Long productId) {
        if (productId == null || productId <= 0) {
            throw new ValidationException("Invalid product ID.");
        }
        return productDAO.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productId));
    }

    @Override
    public Product getProductByCode(String productCode) {
        if (productCode == null || productCode.trim().isEmpty()) {
            throw new ValidationException("Product code cannot be empty.");
        }
        return productDAO.findByProductCode(productCode.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with code: " + productCode));
    }

    @Override
    public List<Product> getAllProducts() {
        return productDAO.findAll();
    }

    @Override
    public List<Product> getActiveProducts() {
        return productDAO.findActiveProducts();
    }

    @Override
    public List<Product> getProductsByCategory(ProductCategory category) {
        if (category == null) {
            return getActiveProducts();
        }
        return productDAO.findByCategory(category);
    }

    @Override
    public void validateProduct(Product product) {
        if (product == null) {
            throw new ValidationException("Product cannot be null.");
        }
        if (product.getProductName() == null || product.getProductName().trim().isEmpty()) {
            throw new ValidationException("Product name is required.");
        }
        if (product.getCategory() == null) {
            throw new ValidationException("Product category is required.");
        }
        if (product.getUnit() == null) {
            throw new ValidationException("Product unit is required.");
        }
        if (product.getPrice() == null || product.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Product price must be greater than zero.");
        }
    }
}
