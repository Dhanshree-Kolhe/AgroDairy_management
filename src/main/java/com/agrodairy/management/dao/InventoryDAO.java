package com.agrodairy.management.dao;

import com.agrodairy.management.model.Inventory;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface InventoryDAO {
    Long insert(Inventory inventory);
    boolean updateStock(Long productId, BigDecimal newStock);
    boolean updateReorderLevels(Long productId, BigDecimal minimumStock, BigDecimal reorderLevel);
    Optional<Inventory> findByProductId(Long productId);
    List<Inventory> findAll();
    List<Inventory> findLowStockItems();
}
