package com.agrodairy.management.controller;

import com.agrodairy.management.dao.InventoryDAO;
import com.agrodairy.management.dao.ProductDAO;
import com.agrodairy.management.dao.impl.InventoryDAOImpl;
import com.agrodairy.management.dao.impl.ProductDAOImpl;
import com.agrodairy.management.model.Inventory;
import com.agrodairy.management.model.Product;
import com.agrodairy.management.util.AlertUtils;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller for the Admin Read-Only Product Catalog View.
 * Displays all dairy products, categories, standard units, prices, and inventory stocks.
 */
public class AdminProductsController {

    private static final Logger logger = LoggerFactory.getLogger(AdminProductsController.class);

    @FXML private TextField searchField;
    @FXML private ComboBox<String> categoryFilterCombo;
    @FXML private Label productCountBadge;

    @FXML private TableView<Product> productsTable;
    @FXML private TableColumn<Product, String> colProductId;
    @FXML private TableColumn<Product, String> colProductCode;
    @FXML private TableColumn<Product, String> colProductName;
    @FXML private TableColumn<Product, String> colCategory;
    @FXML private TableColumn<Product, String> colUnit;
    @FXML private TableColumn<Product, String> colPrice;
    @FXML private TableColumn<Product, String> colStock;
    @FXML private TableColumn<Product, String> colStatus;

    private final ProductDAO productDAO;
    private final InventoryDAO inventoryDAO;
    private FilteredList<Product> filteredProducts;
    private Map<Long, BigDecimal> stockMap;

    public AdminProductsController() {
        this.productDAO = new ProductDAOImpl();
        this.inventoryDAO = new InventoryDAOImpl();
    }

    public AdminProductsController(ProductDAO productDAO, InventoryDAO inventoryDAO) {
        this.productDAO = productDAO;
        this.inventoryDAO = inventoryDAO;
    }

    @FXML
    public void initialize() {
        setupFilters();
        setupTableColumns();
        loadProducts();
    }

    private void setupFilters() {
        if (categoryFilterCombo != null) {
            categoryFilterCombo.setItems(FXCollections.observableArrayList("ALL", "MILK", "DAIRY_PRODUCT", "BY_PRODUCT"));
            categoryFilterCombo.setValue("ALL");
            categoryFilterCombo.valueProperty().addListener((obs, oldV, newV) -> applyFilter());
        }

        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldV, newV) -> applyFilter());
        }
    }

    private void setupTableColumns() {
        if (colProductId != null) {
            colProductId.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getProductId())));
        }
        if (colProductCode != null) {
            colProductCode.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getProductCode()));
        }
        if (colProductName != null) {
            colProductName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getProductName()));
        }
        if (colCategory != null) {
            colCategory.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCategory() != null ? d.getValue().getCategory().name() : ""));
        }
        if (colUnit != null) {
            colUnit.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getUnit() != null ? d.getValue().getUnit().name() : ""));
        }
        if (colPrice != null) {
            colPrice.setCellValueFactory(d -> new SimpleStringProperty("₹" + d.getValue().getPrice().toString()));
        }
        if (colStock != null) {
            colStock.setCellValueFactory(d -> {
                Long pId = d.getValue().getProductId();
                BigDecimal stock = stockMap != null ? stockMap.getOrDefault(pId, BigDecimal.ZERO) : BigDecimal.ZERO;
                String unit = d.getValue().getUnit() != null ? d.getValue().getUnit().name() : "";
                return new SimpleStringProperty(stock.toString() + " " + unit);
            });
            colStock.setCellFactory(col -> new TableCell<>() {
                @Override
                protected void updateItem(String val, boolean empty) {
                    super.updateItem(val, empty);
                    if (empty || val == null) {
                        setText(null);
                        setStyle("");
                    } else {
                        setText(val);
                        // Highlight low stock if starts with small number
                        try {
                            String numStr = val.split(" ")[0];
                            if (new BigDecimal(numStr).compareTo(new BigDecimal("10.00")) <= 0) {
                                setStyle("-fx-text-fill: #D97706; -fx-font-weight: bold;");
                            } else {
                                setStyle("-fx-text-fill: #0F172A;");
                            }
                        } catch (Exception ignored) {
                            setStyle("-fx-text-fill: #0F172A;");
                        }
                    }
                }
            });
        }
        if (colStatus != null) {
            colStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus() != null ? d.getValue().getStatus() : "ACTIVE"));
            colStatus.setCellFactory(column -> new TableCell<>() {
                @Override
                protected void updateItem(String status, boolean empty) {
                    super.updateItem(status, empty);
                    if (empty || status == null) {
                        setText(null);
                        setStyle("");
                    } else {
                        setText(status);
                        if ("ACTIVE".equalsIgnoreCase(status)) {
                            setStyle("-fx-text-fill: #059669; -fx-font-weight: bold;");
                        } else {
                            setStyle("-fx-text-fill: #DC2626; -fx-font-weight: bold;");
                        }
                    }
                }
            });
        }
    }

    @FXML
    public void loadProducts() {
        try {
            List<Product> products = productDAO.findAll();

            // Load stock from inventory
            List<Inventory> inventories = inventoryDAO.findAll();
            stockMap = inventories.stream()
                    .collect(Collectors.toMap(Inventory::getProductId, Inventory::getCurrentStock, (a, b) -> a));

            filteredProducts = new FilteredList<>(FXCollections.observableArrayList(products), p -> true);
            if (productsTable != null) {
                productsTable.setItems(filteredProducts);
            }
            applyFilter();
        } catch (Exception e) {
            logger.error("Failed to load products list", e);
            AlertUtils.showError("Database Error", "Failed to retrieve product records: " + e.getMessage());
        }
    }

    private void applyFilter() {
        if (filteredProducts == null) return;

        String query = searchField != null && searchField.getText() != null ? searchField.getText().trim().toLowerCase() : "";
        String catFilter = categoryFilterCombo != null ? categoryFilterCombo.getValue() : "ALL";

        filteredProducts.setPredicate(p -> {
            if (!query.isEmpty()) {
                boolean matchName = p.getProductName() != null && p.getProductName().toLowerCase().contains(query);
                boolean matchCode = p.getProductCode() != null && p.getProductCode().toLowerCase().contains(query);
                if (!matchName && !matchCode) return false;
            }

            if (catFilter != null && !catFilter.equals("ALL")) {
                if (p.getCategory() == null || !p.getCategory().name().equalsIgnoreCase(catFilter)) {
                    return false;
                }
            }

            return true;
        });

        if (productCountBadge != null) {
            productCountBadge.setText("Showing: " + filteredProducts.size() + " products");
        }
    }

    @FXML
    public void handleResetFilters() {
        if (searchField != null) searchField.clear();
        if (categoryFilterCombo != null) categoryFilterCombo.setValue("ALL");
        loadProducts();
    }
}
