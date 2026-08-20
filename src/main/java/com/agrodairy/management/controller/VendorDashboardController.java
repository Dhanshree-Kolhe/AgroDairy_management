package com.agrodairy.management.controller;

import com.agrodairy.management.model.Customer;
import com.agrodairy.management.model.DeliveryRecord;
import com.agrodairy.management.model.ExtraOrder;
import com.agrodairy.management.model.Inventory;
import com.agrodairy.management.model.MonthlyBill;
import com.agrodairy.management.model.enums.DeliveryShift;
import com.agrodairy.management.model.enums.DeliveryStatus;
import com.agrodairy.management.service.BillingService;
import com.agrodairy.management.service.CustomerService;
import com.agrodairy.management.service.DeliveryService;
import com.agrodairy.management.service.ExtraOrderService;
import com.agrodairy.management.service.InventoryService;
import com.agrodairy.management.service.impl.BillingServiceImpl;
import com.agrodairy.management.service.impl.CustomerServiceImpl;
import com.agrodairy.management.service.impl.DeliveryServiceImpl;
import com.agrodairy.management.service.impl.ExtraOrderServiceImpl;
import com.agrodairy.management.service.impl.InventoryServiceImpl;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.List;

public class VendorDashboardController {

    @FXML private Label todaysCustomersLabel;
    @FXML private Label deliveredLabel;
    @FXML private Label pendingDeliveriesLabel;
    @FXML private Label todaysRevenueLabel;
    @FXML private Label pendingPaymentsLabel;
    @FXML private Label extraOrdersLabel;
    @FXML private Label lowStockAlertsLabel;

    @FXML private TableView<Customer> dashboardTable;
    @FXML private TableColumn<Customer, String> colCustCode;
    @FXML private TableColumn<Customer, String> colCustName;
    @FXML private TableColumn<Customer, String> colArea;
    @FXML private TableColumn<Customer, String> colMilkType;
    @FXML private TableColumn<Customer, String> colShift;
    @FXML private TableColumn<Customer, String> colQuantity;
    @FXML private TableColumn<Customer, String> colRate;
    @FXML private TableColumn<Customer, String> colStatus;

    private final CustomerService customerService;
    private final DeliveryService deliveryService;
    private final BillingService billingService;
    private final ExtraOrderService extraOrderService;
    private final InventoryService inventoryService;

    public VendorDashboardController() {
        this.customerService = new CustomerServiceImpl();
        this.deliveryService = new DeliveryServiceImpl();
        this.billingService = new BillingServiceImpl();
        this.extraOrderService = new ExtraOrderServiceImpl();
        this.inventoryService = new InventoryServiceImpl();
    }

    @FXML
    public void initialize() {
        setupTableColumns();
        refreshDashboard();
    }

    private void setupTableColumns() {
        if (colCustCode != null) colCustCode.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCustomerCode()));
        if (colCustName != null) colCustName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFullName()));
        if (colArea != null) colArea.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getArea()));
        if (colMilkType != null) colMilkType.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getMilkType() != null ? data.getValue().getMilkType().name() : ""));
        if (colShift != null) colShift.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDeliveryShift() != null ? data.getValue().getDeliveryShift().name() : ""));
        if (colQuantity != null) colQuantity.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getMorningQuantity().add(data.getValue().getEveningQuantity()).toString() + " L"));
        if (colRate != null) colRate.setCellValueFactory(data -> new SimpleStringProperty("₹" + data.getValue().getCurrentPricePerLiter().toString()));
        if (colStatus != null) colStatus.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus() != null ? data.getValue().getStatus().name() : ""));
    }

    @FXML
    public void refreshDashboard() {
        List<Customer> customers = customerService.getAllCustomers();
        Date today = new Date(System.currentTimeMillis());

        // 1. Today's Customers
        int totalCustomers = customers.size();
        if (todaysCustomersLabel != null) todaysCustomersLabel.setText(String.valueOf(totalCustomers));

        // 2. Delivered & 3. Pending Deliveries & 4. Today's Revenue
        List<DeliveryRecord> todayRecords = deliveryService.getDeliveriesByDate(today);
        long deliveredCount = todayRecords.stream().filter(r -> r.getStatus() == DeliveryStatus.DELIVERED).count();
        long pendingCount = Math.max(0, totalCustomers - deliveredCount);

        BigDecimal revenue = todayRecords.stream()
                .filter(r -> r.getStatus() == DeliveryStatus.DELIVERED)
                .map(DeliveryRecord::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (deliveredLabel != null) deliveredLabel.setText(String.valueOf(deliveredCount));
        if (pendingDeliveriesLabel != null) pendingDeliveriesLabel.setText(String.valueOf(pendingCount));
        if (todaysRevenueLabel != null) todaysRevenueLabel.setText("₹" + revenue.setScale(2).toString());

        // 5. Pending Payments
        List<MonthlyBill> pendingBills = billingService.getPendingBills();
        if (pendingPaymentsLabel != null) pendingPaymentsLabel.setText(pendingBills.size() + " Bills");

        // 6. Extra Orders
        List<ExtraOrder> pendingOrders = extraOrderService.getPendingOrders();
        if (extraOrdersLabel != null) extraOrdersLabel.setText(pendingOrders.size() + " Pending");

        // 7. Low Stock Alerts
        List<Inventory> lowStock = inventoryService.getLowStockItems();
        if (lowStockAlertsLabel != null) lowStockAlertsLabel.setText(lowStock.size() + " Items");

        // Populate Table
        if (dashboardTable != null) {
            dashboardTable.setItems(FXCollections.observableArrayList(customers));
        }
    }
}
