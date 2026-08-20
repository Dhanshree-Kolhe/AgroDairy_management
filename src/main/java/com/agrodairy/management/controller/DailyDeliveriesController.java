package com.agrodairy.management.controller;

import com.agrodairy.management.model.Customer;
import com.agrodairy.management.model.DeliveryRecord;
import com.agrodairy.management.model.enums.DeliveryShift;
import com.agrodairy.management.model.enums.DeliveryStatus;
import com.agrodairy.management.service.CustomerService;
import com.agrodairy.management.service.DeliveryService;
import com.agrodairy.management.service.impl.CustomerServiceImpl;
import com.agrodairy.management.service.impl.DeliveryServiceImpl;
import com.agrodairy.management.util.AlertUtils;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class DailyDeliveriesController {

    @FXML private DatePicker deliveryDatePicker;
    @FXML private ComboBox<DeliveryShift> shiftComboBox;

    @FXML private TableView<CustomerDeliveryRow> deliveriesTable;
    @FXML private TableColumn<CustomerDeliveryRow, String> colCustCode;
    @FXML private TableColumn<CustomerDeliveryRow, String> colCustName;
    @FXML private TableColumn<CustomerDeliveryRow, String> colArea;
    @FXML private TableColumn<CustomerDeliveryRow, String> colMilkType;
    @FXML private TableColumn<CustomerDeliveryRow, String> colQty;
    @FXML private TableColumn<CustomerDeliveryRow, String> colRate;
    @FXML private TableColumn<CustomerDeliveryRow, String> colTotal;
    @FXML private TableColumn<CustomerDeliveryRow, String> colStatus;
    @FXML private TableColumn<CustomerDeliveryRow, String> colRemarks;

    private final CustomerService customerService;
    private final DeliveryService deliveryService;

    public DailyDeliveriesController() {
        this.customerService = new CustomerServiceImpl();
        this.deliveryService = new DeliveryServiceImpl();
    }

    @FXML
    public void initialize() {
        if (deliveryDatePicker != null) deliveryDatePicker.setValue(LocalDate.now());
        if (shiftComboBox != null) {
            shiftComboBox.setItems(FXCollections.observableArrayList(DeliveryShift.MORNING, DeliveryShift.EVENING));
            shiftComboBox.setValue(DeliveryShift.MORNING);
        }

        setupTable();
        loadShiftDeliveries();
    }

    private void setupTable() {
        colCustCode.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCustomer().getCustomerCode()));
        colCustName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCustomer().getFullName()));
        colArea.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCustomer().getArea()));
        colMilkType.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCustomer().getMilkType().name()));
        colQty.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getScheduledQuantity().toString() + " L"));
        colRate.setCellValueFactory(data -> new SimpleStringProperty("₹" + data.getValue().getSnapshotRate().toString()));
        colTotal.setCellValueFactory(data -> new SimpleStringProperty("₹" + data.getValue().getTotalAmount().toString()));
        colStatus.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus().name()));
        colRemarks.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getRemarks()));
    }

    @FXML
    public void loadShiftDeliveries() {
        LocalDate localDate = deliveryDatePicker.getValue() != null ? deliveryDatePicker.getValue() : LocalDate.now();
        Date sqlDate = Date.valueOf(localDate);
        DeliveryShift shift = shiftComboBox.getValue() != null ? shiftComboBox.getValue() : DeliveryShift.MORNING;

        List<Customer> allCustomers = customerService.getAllCustomers();
        List<DeliveryRecord> existingRecords = deliveryService.getTodayDeliveries(sqlDate, shift);

        List<CustomerDeliveryRow> rows = new ArrayList<>();
        for (Customer c : allCustomers) {
            if (c.getDeliveryShift() == shift || c.getDeliveryShift() == DeliveryShift.BOTH) {
                BigDecimal qty = (shift == DeliveryShift.MORNING) ? c.getMorningQuantity() : c.getEveningQuantity();
                BigDecimal rate = c.getCurrentPricePerLiter();

                // Check existing delivery record if already marked
                DeliveryRecord existing = existingRecords.stream()
                        .filter(r -> r.getCustomerId().equals(c.getCustomerId()))
                        .findFirst().orElse(null);

                DeliveryStatus status = (existing != null) ? existing.getStatus() : DeliveryStatus.NOT_DELIVERED;
                String remarks = (existing != null && existing.getRemarks() != null) ? existing.getRemarks() : "";
                BigDecimal totalAmount = (existing != null) ? existing.getTotalAmount() : qty.multiply(rate);

                rows.add(new CustomerDeliveryRow(c, qty, rate, totalAmount, status, remarks));
            }
        }

        deliveriesTable.setItems(FXCollections.observableArrayList(rows));
    }

    @FXML
    public void handleMarkDelivered() {
        CustomerDeliveryRow row = deliveriesTable.getSelectionModel().getSelectedItem();
        if (row == null) {
            AlertUtils.showWarning("Selection Required", "Please select a customer row to mark as delivered.");
            return;
        }

        Date sqlDate = Date.valueOf(deliveryDatePicker.getValue());
        DeliveryShift shift = shiftComboBox.getValue();

        deliveryService.markDelivered(row.getCustomer().getCustomerId(), sqlDate, shift, "Delivered on time");
        AlertUtils.showInfo("Success", "Delivery marked DELIVERED for " + row.getCustomer().getFullName());
        loadShiftDeliveries();
    }

    @FXML
    public void handleMarkNotDelivered() {
        CustomerDeliveryRow row = deliveriesTable.getSelectionModel().getSelectedItem();
        if (row == null) {
            AlertUtils.showWarning("Selection Required", "Please select a customer row.");
            return;
        }

        Date sqlDate = Date.valueOf(deliveryDatePicker.getValue());
        DeliveryShift shift = shiftComboBox.getValue();

        deliveryService.markNotDelivered(row.getCustomer().getCustomerId(), sqlDate, shift, "Vendor Not Delivered");
        AlertUtils.showInfo("Status Updated", "Marked NOT DELIVERED.");
        loadShiftDeliveries();
    }

    @FXML
    public void handleMarkPaused() {
        CustomerDeliveryRow row = deliveriesTable.getSelectionModel().getSelectedItem();
        if (row == null) {
            AlertUtils.showWarning("Selection Required", "Please select a customer row.");
            return;
        }

        Date sqlDate = Date.valueOf(deliveryDatePicker.getValue());
        DeliveryShift shift = shiftComboBox.getValue();

        deliveryService.markPaused(row.getCustomer().getCustomerId(), sqlDate, shift, "Paused by Request");
        AlertUtils.showInfo("Status Updated", "Marked PAUSED.");
        loadShiftDeliveries();
    }

    // Helper Wrapper Row for UI Table Binding
    public static class CustomerDeliveryRow {
        private final Customer customer;
        private final BigDecimal scheduledQuantity;
        private final BigDecimal snapshotRate;
        private final BigDecimal totalAmount;
        private final DeliveryStatus status;
        private final String remarks;

        public CustomerDeliveryRow(Customer customer, BigDecimal scheduledQuantity, BigDecimal snapshotRate, BigDecimal totalAmount, DeliveryStatus status, String remarks) {
            this.customer = customer;
            this.scheduledQuantity = scheduledQuantity;
            this.snapshotRate = snapshotRate;
            this.totalAmount = totalAmount;
            this.status = status;
            this.remarks = remarks;
        }

        public Customer getCustomer() { return customer; }
        public BigDecimal getScheduledQuantity() { return scheduledQuantity; }
        public BigDecimal getSnapshotRate() { return snapshotRate; }
        public BigDecimal getTotalAmount() { return totalAmount; }
        public DeliveryStatus getStatus() { return status; }
        public String getRemarks() { return remarks; }
    }
}
