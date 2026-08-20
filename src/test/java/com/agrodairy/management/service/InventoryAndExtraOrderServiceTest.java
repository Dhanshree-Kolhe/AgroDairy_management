package com.agrodairy.management.service;

import com.agrodairy.management.dao.InventoryDAO;
import com.agrodairy.management.exception.InsufficientStockException;
import com.agrodairy.management.exception.ValidationException;
import com.agrodairy.management.model.Inventory;
import com.agrodairy.management.service.impl.InventoryServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class InventoryAndExtraOrderServiceTest {

    private InventoryService inventoryService;
    private StubInventoryDAO inventoryDAO;

    @BeforeEach
    public void setUp() {
        inventoryDAO = new StubInventoryDAO();
        inventoryService = new InventoryServiceImpl(inventoryDAO);

        Inventory inv = new Inventory();
        inv.setInventoryId(10L);
        inv.setProductId(3L); // Paneer
        inv.setOpeningStock(new BigDecimal("50.00"));
        inv.setCurrentStock(new BigDecimal("12.00"));
        inv.setMinimumStock(new BigDecimal("5.00"));
        inv.setReorderLevel(new BigDecimal("10.00"));

        inventoryDAO.insert(inv);
    }

    @Test
    public void test10_InventoryCannotBecomeNegative() {
        // Attempt to reduce stock by 15 when available is only 12
        assertThrows(InsufficientStockException.class, () -> {
            inventoryService.reduceStock(3L, new BigDecimal("15.00"));
        });

        // Negative quantity reduction parameter rejection
        assertThrows(ValidationException.class, () -> {
            inventoryService.reduceStock(3L, new BigDecimal("-5.00"));
        });
    }

    @Test
    public void test11_LowStockDetection() {
        assertFalse(inventoryService.isLowStock(3L)); // 12 > 10

        // Reduce stock to 8 (which is <= reorderLevel of 10)
        inventoryService.reduceStock(3L, new BigDecimal("4.00"));

        assertTrue(inventoryService.isLowStock(3L)); // 8 <= 10
        List<Inventory> lowStockList = inventoryService.getLowStockItems();
        assertEquals(1, lowStockList.size());
    }

    private static class StubInventoryDAO implements InventoryDAO {
        private final List<Inventory> list = new ArrayList<>();
        private long seq = 1;

        @Override public Long insert(Inventory inv) { inv.setInventoryId(seq++); list.add(inv); return inv.getInventoryId(); }
        @Override public boolean updateStock(Long productId, BigDecimal newStock) {
            Optional<Inventory> inv = findByProductId(productId);
            if (inv.isPresent()) {
                inv.get().setCurrentStock(newStock);
                return true;
            }
            return false;
        }
        @Override public boolean updateReorderLevels(Long productId, BigDecimal min, BigDecimal reorder) { return true; }
        @Override public Optional<Inventory> findByProductId(Long productId) {
            return list.stream().filter(i -> i.getProductId().equals(productId)).findFirst();
        }
        @Override public List<Inventory> findAll() { return list; }
        @Override public List<Inventory> findLowStockItems() {
            List<Inventory> low = new ArrayList<>();
            for (Inventory i : list) {
                if (i.getCurrentStock().compareTo(i.getReorderLevel()) <= 0) low.add(i);
            }
            return low;
        }
    }
}
