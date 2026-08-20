package com.agrodairy.management.controller;

import com.agrodairy.management.dao.CustomerDAO;
import com.agrodairy.management.dao.ProductDAO;
import com.agrodairy.management.dao.impl.CustomerDAOImpl;
import com.agrodairy.management.dao.impl.ProductDAOImpl;
import com.agrodairy.management.model.Customer;
import com.agrodairy.management.model.ExtraOrder;
import com.agrodairy.management.model.Product;
import com.agrodairy.management.service.ExtraOrderService;
import com.agrodairy.management.service.impl.ExtraOrderServiceImpl;
import com.agrodairy.management.util.AlertUtils;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.List;
import java.util.Optional;

public class ExtraOrdersController {

    @FXML private TableView<ExtraOrderRow> ordersTable;
    @FXML private TableColumn<ExtraOrderRow, String> colOrderId;
    @FXML private TableColumn<ExtraOrderRow, String> colCustName;
    @FXML private TableColumn<ExtraOrderRow, String> colProdName;
    @FXML private TableColumn<ExtraOrderRow, String> colQty;
    @FXML private TableColumn<ExtraOrderRow, String> colSnapshotPrice;
    @FXML private TableColumn<ExtraOrderRow, String> colTotalPrice;
    @FXML private TableColumn<ExtraOrderRow, String> colDeliveryDate;
    @FXML private TableColumn<ExtraOrderRow, String> colStatus;

    private final ExtraOrderService extraOrderService;
    private final CustomerDAO customerDAO;
    private final ProductDAO productDAO;

    public ExtraOrdersController() {
        this.extraOrderService = new ExtraOrderServiceImpl();
        this.customerDAO = new CustomerDAOImpl();
        this.productDAO = new ProductDAOImpl();
    }

    @FXML
    public void initialize() {
        setupTable();
        refreshTable();
    }

    private void setupTable() {
        colOrderId.setCellValueFactory(data -> new SimpleStringProperty("#" + data.getValue().getOrder().getOrderId()));
        colCustName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCustomerName()));
        colProdName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getProductName()));
        colQty.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getOrder().getQuantity() + " " + data.getValue().getUnit()));
        colSnapshotPrice.setCellValueFactory(data -> new SimpleStringProperty("₹" + data.getValue().getOrder().getPriceSnapshot()));
        colTotalPrice.setCellValueFactory(data -> new SimpleStringProperty("₹" + data.getValue().getOrder().getTotalPrice()));
        colDeliveryDate.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getOrder().getDeliveryDate().toString()));
        colStatus.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getOrder().getStatus().name()));
    }

    @FXML
    public void refreshTable() {
        List<ExtraOrder> pending = extraOrderService.getPendingOrders();
        List<ExtraOrderRow> rows = pending.stream().map(ord -> {
            Optional<Customer> c = customerDAO.findById(ord.getCustomerId());
            Optional<Product> p = productDAO.findById(ord.getProductId());
            String cName = c.isPresent() ? c.get().getFullName() : "Customer #" + ord.getCustomerId();
            String pName = p.isPresent() ? p.get().getProductName() : "Product #" + ord.getProductId();
            String unit = p.isPresent() ? p.get().getUnit().name() : "";
            return new ExtraOrderRow(ord, cName, pName, unit);
        }).toList();

        ordersTable.setItems(FXCollections.observableArrayList(rows));
    }

    @FXML
    public void handleApproveOrder() {
        ExtraOrderRow sel = ordersTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            AlertUtils.showWarning("Selection Required", "Please select an extra order to approve.");
            return;
        }

        try {
            extraOrderService.approveOrder(sel.getOrder().getOrderId());
            AlertUtils.showInfo("Order Approved", "Extra Order approved and stock automatically deducted!");
            refreshTable();
        } catch (Exception e) {
            AlertUtils.showError("Approval Error", e.getMessage());
        }
    }

    @FXML
    public void handleRejectOrder() {
        ExtraOrderRow sel = ordersTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            AlertUtils.showWarning("Selection Required", "Please select an extra order to reject.");
            return;
        }

        try {
            extraOrderService.rejectOrder(sel.getOrder().getOrderId());
            AlertUtils.showInfo("Order Rejected", "Extra Order status set to REJECTED.");
            refreshTable();
        } catch (Exception e) {
            AlertUtils.showError("Error", e.getMessage());
        }
    }

    public static class ExtraOrderRow {
        private final ExtraOrder order;
        private final String customerName;
        private final String productName;
        private final String unit;

        public ExtraOrderRow(ExtraOrder order, String customerName, String productName, String unit) {
            this.order = order;
            this.customerName = customerName;
            this.productName = productName;
            this.unit = unit;
        }

        public ExtraOrder getOrder() { return order; }
        public String getCustomerName() { return customerName; }
        public String getProductName() { return productName; }
        public String getUnit() { return unit; }
    }
}
