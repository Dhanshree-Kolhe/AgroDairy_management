package com.agrodairy.management.dao.impl;

import com.agrodairy.management.dao.ProductDAO;
import com.agrodairy.management.exception.DatabaseException;
import com.agrodairy.management.model.Product;
import com.agrodairy.management.model.enums.ProductCategory;
import com.agrodairy.management.model.enums.ProductUnit;
import com.agrodairy.management.util.DatabaseUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProductDAOImpl implements ProductDAO {

    @Override
    public Long insert(Product product) {
        String sql = "INSERT INTO products (product_code, product_name, category, unit, price, status) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, product.getProductCode());
            stmt.setString(2, product.getProductName());
            stmt.setString(3, product.getCategory().name());
            stmt.setString(4, product.getUnit().name());
            stmt.setBigDecimal(5, product.getPrice());
            stmt.setString(6, product.getStatus() != null ? product.getStatus() : "ACTIVE");

            int affected = stmt.executeUpdate();
            if (affected == 0) throw new DatabaseException("Creating product failed, no rows affected.");

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    long id = generatedKeys.getLong(1);
                    product.setProductId(id);
                    return id;
                } else {
                    throw new DatabaseException("Creating product failed, no ID obtained.");
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to insert product: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean update(Product product) {
        String sql = "UPDATE products SET product_name = ?, category = ?, unit = ?, price = ?, status = ? WHERE product_id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, product.getProductName());
            stmt.setString(2, product.getCategory().name());
            stmt.setString(3, product.getUnit().name());
            stmt.setBigDecimal(4, product.getPrice());
            stmt.setString(5, product.getStatus());
            stmt.setLong(6, product.getProductId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update product: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean delete(Long productId) {
        String sql = "UPDATE products SET status = 'INACTIVE' WHERE product_id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, productId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to deactivate product: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Product> findById(Long productId) {
        String sql = "SELECT * FROM products WHERE product_id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, productId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapResultSetToProduct(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find product by ID: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Product> findByProductCode(String productCode) {
        String sql = "SELECT * FROM products WHERE product_code = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, productCode);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapResultSetToProduct(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find product by code: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public List<Product> findAll() {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT * FROM products ORDER BY product_id ASC";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                products.add(mapResultSetToProduct(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to retrieve products: " + e.getMessage(), e);
        }
        return products;
    }

    @Override
    public List<Product> findByCategory(ProductCategory category) {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT * FROM products WHERE category = ? ORDER BY product_name ASC";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, category.name());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    products.add(mapResultSetToProduct(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find products by category: " + e.getMessage(), e);
        }
        return products;
    }

    @Override
    public List<Product> findActiveProducts() {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT * FROM products WHERE status = 'ACTIVE' ORDER BY product_name ASC";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                products.add(mapResultSetToProduct(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find active products: " + e.getMessage(), e);
        }
        return products;
    }

    private Product mapResultSetToProduct(ResultSet rs) throws SQLException {
        Product p = new Product();
        p.setProductId(rs.getLong("product_id"));
        p.setProductCode(rs.getString("product_code"));
        p.setProductName(rs.getString("product_name"));
        p.setCategory(ProductCategory.valueOf(rs.getString("category")));
        p.setUnit(ProductUnit.valueOf(rs.getString("unit")));
        p.setPrice(rs.getBigDecimal("price"));
        p.setStatus(rs.getString("status"));
        p.setCreatedAt(rs.getTimestamp("created_at"));
        return p;
    }
}
