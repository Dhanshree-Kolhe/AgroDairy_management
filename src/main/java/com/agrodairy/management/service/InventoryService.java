package com.agrodairy.management.service;

import com.agrodairy.management.model.Inventory;

import java.math.BigDecimal;
import java.util.List;

public interface InventoryService {
    Inventory getInventoryByProduct(Long productId);
    List<Inventory> getAllInventory();
    List<Inventory> getLowStockItems();
    boolean addStock(Long productId, BigDecimal quantityToAdd);
    boolean reduceStock(Long productId, BigDecimal quantityToReduce);
    boolean updateReorderLevels(Long productId, BigDecimal minimumStock, BigDecimal reorderLevel);
    boolean isLowStock(Long productId);
}
