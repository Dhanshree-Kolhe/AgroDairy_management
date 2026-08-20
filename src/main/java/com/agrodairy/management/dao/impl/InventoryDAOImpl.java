package com.agrodairy.management.dao.impl;

import com.agrodairy.management.dao.InventoryDAO;
import com.agrodairy.management.exception.DatabaseException;
import com.agrodairy.management.model.Inventory;
import com.agrodairy.management.util.DatabaseUtil;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class InventoryDAOImpl implements InventoryDAO {

    @Override
    public Long insert(Inventory inv) {
        String sql = "INSERT INTO inventory (product_id, opening_stock, current_stock, minimum_stock, reorder_level) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, inv.getProductId());
            stmt.setBigDecimal(2, inv.getOpeningStock());
            stmt.setBigDecimal(3, inv.getCurrentStock());
            stmt.setBigDecimal(4, inv.getMinimumStock());
            stmt.setBigDecimal(5, inv.getReorderLevel());

            int affected = stmt.executeUpdate();
            if (affected == 0) throw new DatabaseException("Creating inventory entry failed, no rows affected.");

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    long id = generatedKeys.getLong(1);
                    inv.setInventoryId(id);
                    return id;
                } else {
                    throw new DatabaseException("Creating inventory failed, no ID obtained.");
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to insert inventory: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean updateStock(Long productId, BigDecimal newStock) {
        String sql = "UPDATE inventory SET current_stock = ? WHERE product_id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBigDecimal(1, newStock);
            stmt.setLong(2, productId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update inventory stock: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean updateReorderLevels(Long productId, BigDecimal minimumStock, BigDecimal reorderLevel) {
        String sql = "UPDATE inventory SET minimum_stock = ?, reorder_level = ? WHERE product_id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBigDecimal(1, minimumStock);
            stmt.setBigDecimal(2, reorderLevel);
            stmt.setLong(3, productId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update inventory reorder levels: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Inventory> findByProductId(Long productId) {
        String sql = "SELECT * FROM inventory WHERE product_id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, productId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapResultSetToInventory(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find inventory by product ID: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public List<Inventory> findAll() {
        List<Inventory> items = new ArrayList<>();
        String sql = "SELECT * FROM inventory ORDER BY inventory_id ASC";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                items.add(mapResultSetToInventory(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to retrieve inventory items: " + e.getMessage(), e);
        }
        return items;
    }

    @Override
    public List<Inventory> findLowStockItems() {
        List<Inventory> items = new ArrayList<>();
        String sql = "SELECT * FROM inventory WHERE current_stock <= reorder_level ORDER BY current_stock ASC";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                items.add(mapResultSetToInventory(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to retrieve low-stock inventory items: " + e.getMessage(), e);
        }
        return items;
    }

    private Inventory mapResultSetToInventory(ResultSet rs) throws SQLException {
        Inventory inv = new Inventory();
        inv.setInventoryId(rs.getLong("inventory_id"));
        inv.setProductId(rs.getLong("product_id"));
        inv.setOpeningStock(rs.getBigDecimal("opening_stock"));
        inv.setCurrentStock(rs.getBigDecimal("current_stock"));
        inv.setMinimumStock(rs.getBigDecimal("minimum_stock"));
        inv.setReorderLevel(rs.getBigDecimal("reorder_level"));
        inv.setLastUpdated(rs.getTimestamp("last_updated"));
        return inv;
    }
}
