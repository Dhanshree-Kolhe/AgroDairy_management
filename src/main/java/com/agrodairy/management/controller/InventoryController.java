package com.agrodairy.management.controller;

import com.agrodairy.management.dao.ProductDAO;
import com.agrodairy.management.dao.impl.ProductDAOImpl;
import com.agrodairy.management.model.Inventory;
import com.agrodairy.management.model.Product;
import com.agrodairy.management.service.InventoryService;
import com.agrodairy.management.service.impl.InventoryServiceImpl;
import com.agrodairy.management.util.AlertUtils;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public class InventoryController {

    @FXML private TableView<InventoryRow> inventoryTable;
    @FXML private TableColumn<InventoryRow, String> colProdCode;
    @FXML private TableColumn<InventoryRow, String> colProdName;
    @FXML private TableColumn<InventoryRow, String> colCategory;
    @FXML private TableColumn<InventoryRow, String> colUnit;
    @FXML private TableColumn<InventoryRow, String> colPrice;
    @FXML private TableColumn<InventoryRow, String> colCurrentStock;
    @FXML private TableColumn<InventoryRow, String> colReorderLevel;
    @FXML private TableColumn<InventoryRow, String> colStockStatus;

    private final InventoryService inventoryService;
    private final ProductDAO productDAO;

    public InventoryController() {
        this.inventoryService = new InventoryServiceImpl();
        this.productDAO = new ProductDAOImpl();
    }

    @FXML
    public void initialize() {
        setupTable();
        refreshTable();
    }

    private void setupTable() {
        colProdCode.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getProduct().getProductCode()));
        colProdName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getProduct().getProductName()));
        colCategory.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getProduct().getCategory().name()));
        colUnit.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getProduct().getUnit().name()));
        colPrice.setCellValueFactory(data -> new SimpleStringProperty("₹" + data.getValue().getProduct().getPrice().toString()));
        colCurrentStock.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getInventory().getCurrentStock().toString()));
        colReorderLevel.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getInventory().getReorderLevel().toString()));
        colStockStatus.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().isLowStock() ? "⚠️ LOW STOCK ALERT" : "✅ NORMAL STOCK"));
    }

    @FXML
    public void refreshTable() {
        List<Inventory> invList = inventoryService.getAllInventory();
        List<InventoryRow> rows = invList.stream().map(inv -> {
            Optional<Product> prodOpt = productDAO.findById(inv.getProductId());
            Product prod = prodOpt.orElse(new Product());
            boolean low = inv.getCurrentStock().compareTo(inv.getReorderLevel()) <= 0;
            return new InventoryRow(inv, prod, low);
        }).toList();

        inventoryTable.setItems(FXCollections.observableArrayList(rows));
    }

    @FXML
    public void handleAddStock() {
        InventoryRow selected = inventoryTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtils.showWarning("Selection Required", "Please select a product from table to add stock.");
            return;
        }

        TextInputDialog dialog = new TextInputDialog("10");
        dialog.setTitle("Restock Product Quantity");
        dialog.setHeaderText("Add Restock Quantity for: " + selected.getProduct().getProductName());
        dialog.setContentText("Enter Quantity to Add (" + selected.getProduct().getUnit() + "):");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            try {
                BigDecimal qtyToAdd = new BigDecimal(result.get().trim());
                inventoryService.addStock(selected.getInventory().getProductId(), qtyToAdd);
                AlertUtils.showInfo("Stock Updated", "Successfully added " + qtyToAdd + " " + selected.getProduct().getUnit() + " to stock.");
                refreshTable();
            } catch (Exception e) {
                AlertUtils.showError("Stock Update Error", e.getMessage());
            }
        }
    }

    public static class InventoryRow {
        private final Inventory inventory;
        private final Product product;
        private final boolean lowStock;

        public InventoryRow(Inventory inventory, Product product, boolean lowStock) {
            this.inventory = inventory;
            this.product = product;
            this.lowStock = lowStock;
        }

        public Inventory getInventory() { return inventory; }
        public Product getProduct() { return product; }
        public boolean isLowStock() { return lowStock; }
    }
}
