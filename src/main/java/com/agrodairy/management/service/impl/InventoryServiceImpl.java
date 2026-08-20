package com.agrodairy.management.service.impl;

import com.agrodairy.management.dao.InventoryDAO;
import com.agrodairy.management.dao.impl.InventoryDAOImpl;
import com.agrodairy.management.exception.InsufficientStockException;
import com.agrodairy.management.exception.ResourceNotFoundException;
import com.agrodairy.management.exception.ValidationException;
import com.agrodairy.management.model.Inventory;
import com.agrodairy.management.service.InventoryService;

import java.math.BigDecimal;
import java.util.List;

public class InventoryServiceImpl implements InventoryService {
    private final InventoryDAO inventoryDAO;

    public InventoryServiceImpl() {
        this.inventoryDAO = new InventoryDAOImpl();
    }

    public InventoryServiceImpl(InventoryDAO inventoryDAO) {
        this.inventoryDAO = inventoryDAO;
    }

    @Override
    public Inventory getInventoryByProduct(Long productId) {
        if (productId == null || productId <= 0) throw new ValidationException("Invalid product ID.");
        return inventoryDAO.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory record not found for product ID: " + productId));
    }

    @Override
    public List<Inventory> getAllInventory() {
        return inventoryDAO.findAll();
    }

    @Override
    public List<Inventory> getLowStockItems() {
        return inventoryDAO.findLowStockItems();
    }

    @Override
    public boolean addStock(Long productId, BigDecimal quantityToAdd) {
        if (quantityToAdd == null || quantityToAdd.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Quantity to add must be greater than zero.");
        }
        Inventory inv = getInventoryByProduct(productId);
        BigDecimal newStock = inv.getCurrentStock().add(quantityToAdd);
        return inventoryDAO.updateStock(productId, newStock);
    }

    @Override
    public boolean reduceStock(Long productId, BigDecimal quantityToReduce) {
        if (quantityToReduce == null || quantityToReduce.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Quantity to reduce must be greater than zero.");
        }
        Inventory inv = getInventoryByProduct(productId);
        if (inv.getCurrentStock().compareTo(quantityToReduce) < 0) {
            throw new InsufficientStockException(String.format("Insufficient stock for product ID %d. Requested: %s, Available: %s",
                    productId, quantityToReduce, inv.getCurrentStock()));
        }
        BigDecimal newStock = inv.getCurrentStock().subtract(quantityToReduce);
        return inventoryDAO.updateStock(productId, newStock);
    }

    @Override
    public boolean updateReorderLevels(Long productId, BigDecimal minimumStock, BigDecimal reorderLevel) {
        if (productId == null || productId <= 0) throw new ValidationException("Invalid product ID.");
        if (minimumStock == null || minimumStock.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("Minimum stock cannot be negative.");
        }
        if (reorderLevel == null || reorderLevel.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("Reorder level cannot be negative.");
        }
        getInventoryByProduct(productId); // Ensure exists
        return inventoryDAO.updateReorderLevels(productId, minimumStock, reorderLevel);
    }

    @Override
    public boolean isLowStock(Long productId) {
        Inventory inv = getInventoryByProduct(productId);
        return inv.isLowStock();
    }
}
