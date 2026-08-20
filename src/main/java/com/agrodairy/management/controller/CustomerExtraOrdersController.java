package com.agrodairy.management.controller;

import com.agrodairy.management.dao.ProductDAO;
import com.agrodairy.management.dao.impl.ProductDAOImpl;
import com.agrodairy.management.model.Customer;
import com.agrodairy.management.model.ExtraOrder;
import com.agrodairy.management.model.Product;
import com.agrodairy.management.service.ExtraOrderService;
import com.agrodairy.management.service.impl.ExtraOrderServiceImpl;
import com.agrodairy.management.util.AlertUtils;
import com.agrodairy.management.util.SessionManager;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class CustomerExtraOrdersController {

    @FXML private ComboBox<Product> productComboBox;
    @FXML private TextField quantityField;
    @FXML private DatePicker deliveryDatePicker;

    @FXML private TableView<ExtraOrderRow> ordersTable;
    @FXML private TableColumn<ExtraOrderRow, String> colOrderId;
    @FXML private TableColumn<ExtraOrderRow, String> colProductName;
    @FXML private TableColumn<ExtraOrderRow, String> colQty;
    @FXML private TableColumn<ExtraOrderRow, String> colSnapshotPrice;
    @FXML private TableColumn<ExtraOrderRow, String> colTotal;
    @FXML private TableColumn<ExtraOrderRow, String> colDeliveryDate;
    @FXML private TableColumn<ExtraOrderRow, String> colStatus;

    private final ExtraOrderService extraOrderService;
    private final ProductDAO productDAO;

    public CustomerExtraOrdersController() {
        this.extraOrderService = new ExtraOrderServiceImpl();
        this.productDAO = new ProductDAOImpl();
    }

    @FXML
    public void initialize() {
        if (deliveryDatePicker != null) deliveryDatePicker.setValue(LocalDate.now().plusDays(1));
        populateProducts();
        setupTable();
        loadOrders();
    }

    private void populateProducts() {
        List<Product> products = productDAO.findActiveProducts();
        if (productComboBox != null) {
            productComboBox.setItems(FXCollections.observableArrayList(products));
            productComboBox.setConverter(new StringConverter<Product>() {
                @Override
                public String toString(Product p) {
                    return p != null ? String.format("%s (₹%s / %s)", p.getProductName(), p.getPrice(), p.getUnit()) : "";
                }

                @Override
                public Product fromString(String string) { return null; }
            });
            if (!products.isEmpty()) productComboBox.setValue(products.get(0));
        }
    }

    private void setupTable() {
        colOrderId.setCellValueFactory(data -> new SimpleStringProperty("#" + data.getValue().getOrder().getOrderId()));
        colProductName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getProductName()));
        colQty.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getOrder().getQuantity() + " " + data.getValue().getUnit()));
        colSnapshotPrice.setCellValueFactory(data -> new SimpleStringProperty("₹" + data.getValue().getOrder().getPriceSnapshot()));
        colTotal.setCellValueFactory(data -> new SimpleStringProperty("₹" + data.getValue().getOrder().getTotalPrice()));
        colDeliveryDate.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getOrder().getDeliveryDate().toString()));
        colStatus.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getOrder().getStatus().name()));
    }

    private void loadOrders() {
        Customer c = SessionManager.getCurrentCustomer();
        Long cId = c != null ? c.getCustomerId() : 1L;

        List<ExtraOrder> list = extraOrderService.getCustomerOrders(cId);
        List<ExtraOrderRow> rows = list.stream().map(ord -> {
            Optional<Product> p = productDAO.findById(ord.getProductId());
            String pName = p.isPresent() ? p.get().getProductName() : "Product #" + ord.getProductId();
            String unit = p.isPresent() ? p.get().getUnit().name() : "";
            return new ExtraOrderRow(ord, pName, unit);
        }).toList();

        ordersTable.setItems(FXCollections.observableArrayList(rows));
    }

    @FXML
    public void handlePlaceOrder() {
        try {
            Customer c = SessionManager.getCurrentCustomer();
            Long cId = c != null ? c.getCustomerId() : 1L;

            Product product = productComboBox.getValue();
            if (product == null) {
                AlertUtils.showWarning("Missing Product", "Please select a product from dropdown.");
                return;
            }

            BigDecimal qty = new BigDecimal(quantityField.getText().trim());
            Date deliveryDate = Date.valueOf(deliveryDatePicker.getValue());

            extraOrderService.createExtraOrder(cId, product.getProductId(), qty, deliveryDate);
            AlertUtils.showInfo("Order Placed", "Your extra product order for " + product.getProductName() + " has been submitted to vendor!");

            quantityField.clear();
            loadOrders();
        } catch (Exception e) {
            AlertUtils.showError("Order Error", e.getMessage());
        }
    }

    public static class ExtraOrderRow {
        private final ExtraOrder order;
        private final String productName;
        private final String unit;

        public ExtraOrderRow(ExtraOrder order, String productName, String unit) {
            this.order = order;
            this.productName = productName;
            this.unit = unit;
        }

        public ExtraOrder getOrder() { return order; }
        public String getProductName() { return productName; }
        public String getUnit() { return unit; }
    }
}
