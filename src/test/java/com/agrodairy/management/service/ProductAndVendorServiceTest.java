package com.agrodairy.management.service;

import com.agrodairy.management.controller.RoutePlannerController;
import com.agrodairy.management.dao.InventoryDAO;
import com.agrodairy.management.dao.ProductDAO;
import com.agrodairy.management.dao.VendorDAO;
import com.agrodairy.management.exception.InsufficientStockException;
import com.agrodairy.management.exception.ResourceNotFoundException;
import com.agrodairy.management.exception.ValidationException;
import com.agrodairy.management.model.Customer;
import com.agrodairy.management.model.Inventory;
import com.agrodairy.management.model.Product;
import com.agrodairy.management.model.Vendor;
import com.agrodairy.management.model.enums.ProductCategory;
import com.agrodairy.management.model.enums.ProductUnit;
import com.agrodairy.management.service.impl.ProductServiceImpl;
import com.agrodairy.management.service.impl.VendorServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class ProductAndVendorServiceTest {

    private ProductService productService;
    private VendorService vendorService;
    private StubProductDAO productDAO;
    private StubInventoryDAO inventoryDAO;
    private StubVendorDAO vendorDAO;

    @BeforeEach
    public void setUp() {
        productDAO = new StubProductDAO();
        inventoryDAO = new StubInventoryDAO();
        vendorDAO = new StubVendorDAO();

        productService = new ProductServiceImpl(productDAO, inventoryDAO);
        vendorService = new VendorServiceImpl(vendorDAO);
    }

    @Test
    @DisplayName("Product Validation Rejects Invalid Inputs")
    public void testProductValidation() {
        // Null product
        assertThrows(ValidationException.class, () -> productService.addProduct(null));

        // Empty product name
        Product p1 = new Product(null, "P01", "", ProductCategory.DAIRY_PRODUCT, ProductUnit.KG, new BigDecimal("100"), "ACTIVE");
        assertThrows(ValidationException.class, () -> productService.addProduct(p1));

        // Null category
        Product p2 = new Product(null, "P02", "Paneer", null, ProductUnit.KG, new BigDecimal("100"), "ACTIVE");
        assertThrows(ValidationException.class, () -> productService.addProduct(p2));

        // Null unit
        Product p3 = new Product(null, "P03", "Paneer", ProductCategory.DAIRY_PRODUCT, null, new BigDecimal("100"), "ACTIVE");
        assertThrows(ValidationException.class, () -> productService.addProduct(p3));

        // Zero or negative price
        Product p4 = new Product(null, "P04", "Paneer", ProductCategory.DAIRY_PRODUCT, ProductUnit.KG, BigDecimal.ZERO, "ACTIVE");
        assertThrows(ValidationException.class, () -> productService.addProduct(p4));

        Product p5 = new Product(null, "P05", "Paneer", ProductCategory.DAIRY_PRODUCT, ProductUnit.KG, new BigDecimal("-50"), "ACTIVE");
        assertThrows(ValidationException.class, () -> productService.addProduct(p5));
    }

    @Test
    @DisplayName("Product Add & Inventory Auto-Initialization")
    public void testProductAddAndInventoryInit() {
        Product p = new Product(null, null, "Fresh Shrikhand", ProductCategory.DAIRY_PRODUCT, ProductUnit.PACKET, new BigDecimal("140.00"), "ACTIVE");
        Product saved = productService.addProduct(p);

        assertNotNull(saved.getProductId());
        assertNotNull(saved.getProductCode());
        assertTrue(saved.getProductCode().startsWith("PROD-"));

        // Verify inventory record was auto-initialized
        Optional<Inventory> inv = inventoryDAO.findByProductId(saved.getProductId());
        assertTrue(inv.isPresent());
        assertEquals(BigDecimal.ZERO, inv.get().getCurrentStock());
        assertEquals(new BigDecimal("10.00"), inv.get().getMinimumStock());
    }

    @Test
    @DisplayName("Vendor Validation and CRUD Operations")
    public void testVendorValidationAndCrud() {
        // Null vendor
        assertThrows(ValidationException.class, () -> vendorService.addVendor(null));

        // Invalid phone number (not 10 digits)
        Vendor v1 = new Vendor(null, 2L, "V01", "Ramesh Dairy", "Ramesh Sharma", "98220", "ramesh@dairy.com", "Nagpur", null);
        assertThrows(ValidationException.class, () -> vendorService.addVendor(v1));

        // Invalid email format
        Vendor v2 = new Vendor(null, 2L, "V02", "Ramesh Dairy", "Ramesh Sharma", "9822011223", "invalid-email", "Nagpur", null);
        assertThrows(ValidationException.class, () -> vendorService.addVendor(v2));

        // Invalid GST length
        Vendor v3 = new Vendor(null, 2L, "V03", "Ramesh Dairy", "Ramesh Sharma", "9822011223", "ramesh@dairy.com", "Nagpur", "INVALIDGST");
        assertThrows(ValidationException.class, () -> vendorService.addVendor(v3));

        // Valid Vendor Creation
        Vendor vValid = new Vendor(null, 2L, null, "Nagpur Fresh Dairy", "Ramesh Sharma", "9822011223", "ramesh@dairy.com", "Dharampeth, Nagpur", "27AAAAA0000A1Z5");
        Vendor saved = vendorService.addVendor(vValid);

        assertNotNull(saved.getVendorId());
        assertNotNull(saved.getVendorCode());
        assertTrue(saved.getVendorCode().startsWith("VEND-"));

        // Retrieve by userId
        Vendor retrieved = vendorService.getVendorByUserId(2L);
        assertEquals("Nagpur Fresh Dairy", retrieved.getBusinessName());

        // Update vendor
        retrieved.setBusinessName("Nagpur Premium Dairy");
        vendorService.updateVendor(retrieved);
        assertEquals("Nagpur Premium Dairy", vendorService.getVendorById(saved.getVendorId()).getBusinessName());
    }

    @Test
    @DisplayName("Route Planner Haversine Distance Calculation Test")
    public void testHaversineDistanceCalculation() {
        // Nagpur Dharampeth (21.1415, 79.0634) to Nagpur Sitabuldi (21.1458, 79.0882)
        double distanceKm = RoutePlannerController.calculateHaversineKm(21.1415, 79.0634, 21.1458, 79.0882);
        assertTrue(distanceKm > 2.0 && distanceKm < 3.5, "Distance between Dharampeth and Sitabuldi should be approx 2.6 km, got: " + distanceKm);

        // Same point distance must be 0
        assertEquals(0.0, RoutePlannerController.calculateHaversineKm(21.1415, 79.0634, 21.1415, 79.0634), 0.0001);
    }

    // --- In-Memory Stubs ---

    private static class StubProductDAO implements ProductDAO {
        private final List<Product> list = new ArrayList<>();
        private long seq = 1;

        @Override public Long insert(Product p) { p.setProductId(seq++); list.add(p); return p.getProductId(); }
        @Override public boolean update(Product p) { return true; }
        @Override public boolean delete(Long id) {
            findById(id).ifPresent(p -> p.setStatus("INACTIVE"));
            return true;
        }
        @Override public Optional<Product> findById(Long id) { return list.stream().filter(p -> p.getProductId().equals(id)).findFirst(); }
        @Override public Optional<Product> findByProductCode(String code) { return list.stream().filter(p -> p.getProductCode().equalsIgnoreCase(code)).findFirst(); }
        @Override public List<Product> findAll() { return new ArrayList<>(list); }
        @Override public List<Product> findByCategory(ProductCategory cat) { return list.stream().filter(p -> p.getCategory() == cat).toList(); }
        @Override public List<Product> findActiveProducts() { return list.stream().filter(p -> "ACTIVE".equalsIgnoreCase(p.getStatus())).toList(); }
    }

    private static class StubInventoryDAO implements InventoryDAO {
        private final List<Inventory> list = new ArrayList<>();
        private long seq = 1;

        @Override public Long insert(Inventory inv) { inv.setInventoryId(seq++); list.add(inv); return inv.getInventoryId(); }
        @Override public boolean updateStock(Long pId, BigDecimal newStock) {
            findByProductId(pId).ifPresent(i -> i.setCurrentStock(newStock));
            return true;
        }
        @Override public boolean updateReorderLevels(Long pId, BigDecimal minStock, BigDecimal reorder) {
            findByProductId(pId).ifPresent(i -> { i.setMinimumStock(minStock); i.setReorderLevel(reorder); });
            return true;
        }
        @Override public Optional<Inventory> findByProductId(Long pId) { return list.stream().filter(i -> i.getProductId().equals(pId)).findFirst(); }
        @Override public List<Inventory> findAll() { return new ArrayList<>(list); }
        @Override public List<Inventory> findLowStockItems() { return list.stream().filter(Inventory::isLowStock).toList(); }
    }

    private static class StubVendorDAO implements VendorDAO {
        private final List<Vendor> list = new ArrayList<>();
        private long seq = 1;

        @Override public Long insert(Vendor v) { v.setVendorId(seq++); list.add(v); return v.getVendorId(); }
        @Override public boolean update(Vendor v) { return true; }
        @Override public boolean delete(Long id) { list.removeIf(v -> v.getVendorId().equals(id)); return true; }
        @Override public Optional<Vendor> findById(Long id) { return list.stream().filter(v -> v.getVendorId().equals(id)).findFirst(); }
        @Override public Optional<Vendor> findByUserId(Long uId) { return list.stream().filter(v -> v.getUserId() != null && v.getUserId().equals(uId)).findFirst(); }
        @Override public Optional<Vendor> findByVendorCode(String code) { return list.stream().filter(v -> v.getVendorCode().equalsIgnoreCase(code)).findFirst(); }
        @Override public List<Vendor> findAll() { return new ArrayList<>(list); }
    }
}
