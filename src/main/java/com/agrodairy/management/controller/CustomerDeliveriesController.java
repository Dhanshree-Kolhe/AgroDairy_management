package com.agrodairy.management.controller;

import com.agrodairy.management.model.Customer;
import com.agrodairy.management.model.DeliveryRecord;
import com.agrodairy.management.service.DeliveryService;
import com.agrodairy.management.service.impl.DeliveryServiceImpl;
import com.agrodairy.management.util.SessionManager;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class CustomerDeliveriesController {

    @FXML private ComboBox<Integer> monthComboBox;
    @FXML private TextField yearField;
    @FXML private Label totalQtyLabel;

    @FXML private TableView<DeliveryRecord> deliveriesTable;
    @FXML private TableColumn<DeliveryRecord, String> colDate;
    @FXML private TableColumn<DeliveryRecord, String> colShift;
    @FXML private TableColumn<DeliveryRecord, String> colQty;
    @FXML private TableColumn<DeliveryRecord, String> colRate;
    @FXML private TableColumn<DeliveryRecord, String> colTotal;
    @FXML private TableColumn<DeliveryRecord, String> colStatus;
    @FXML private TableColumn<DeliveryRecord, String> colRemarks;

    private final DeliveryService deliveryService;

    public CustomerDeliveriesController() {
        this.deliveryService = new DeliveryServiceImpl();
    }

    @FXML
    public void initialize() {
        if (monthComboBox != null) {
            monthComboBox.setItems(FXCollections.observableArrayList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12));
            monthComboBox.setValue(LocalDate.now().getMonthValue());
        }
        if (yearField != null) yearField.setText(String.valueOf(LocalDate.now().getYear()));

        setupTable();
        loadDeliveries();
    }

    private void setupTable() {
        colDate.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDeliveryDate().toString()));
        colShift.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDeliveryShift().name()));
        colQty.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDeliveredQuantity().toString() + " L"));
        colRate.setCellValueFactory(data -> new SimpleStringProperty("₹" + data.getValue().getRatePerLiter().toString()));
        colTotal.setCellValueFactory(data -> new SimpleStringProperty("₹" + data.getValue().getTotalAmount().toString()));
        colStatus.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus().name()));
        colRemarks.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getRemarks()));
    }

    @FXML
    public void loadDeliveries() {
        Customer c = SessionManager.getCurrentCustomer();
        Long cId = c != null ? c.getCustomerId() : 1L;

        int month = monthComboBox.getValue() != null ? monthComboBox.getValue() : LocalDate.now().getMonthValue();
        int year = Integer.parseInt(yearField.getText().trim());

        List<DeliveryRecord> list = deliveryService.getCustomerDeliveryHistory(cId, month, year);
        deliveriesTable.setItems(FXCollections.observableArrayList(list));

        BigDecimal totalQty = list.stream()
                .filter(r -> r.getStatus() == com.agrodairy.management.model.enums.DeliveryStatus.DELIVERED)
                .map(DeliveryRecord::getDeliveredQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalQtyLabel != null) {
            totalQtyLabel.setText(String.format("Total Delivered Qty: %.2f L", totalQty));
        }
    }
}
