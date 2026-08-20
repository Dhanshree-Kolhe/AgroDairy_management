package com.agrodairy.management.controller;

import com.agrodairy.management.model.Customer;
import com.agrodairy.management.model.MonthlyBill;
import com.agrodairy.management.service.BillingService;
import com.agrodairy.management.service.impl.BillingServiceImpl;
import com.agrodairy.management.util.SessionManager;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.List;

public class CustomerBillsController {

    @FXML private TableView<MonthlyBill> billsTable;
    @FXML private TableColumn<MonthlyBill, String> colBillNum;
    @FXML private TableColumn<MonthlyBill, String> colPeriod;
    @FXML private TableColumn<MonthlyBill, String> colMilkQty;
    @FXML private TableColumn<MonthlyBill, String> colMilkChg;
    @FXML private TableColumn<MonthlyBill, String> colExtraChg;
    @FXML private TableColumn<MonthlyBill, String> colNet;
    @FXML private TableColumn<MonthlyBill, String> colPaid;
    @FXML private TableColumn<MonthlyBill, String> colDue;
    @FXML private TableColumn<MonthlyBill, String> colStatus;

    private final BillingService billingService;

    public CustomerBillsController() {
        this.billingService = new BillingServiceImpl();
    }

    @FXML
    public void initialize() {
        setupTable();
        loadBills();
    }

    private void setupTable() {
        colBillNum.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getBillNumber()));
        colPeriod.setCellValueFactory(data -> new SimpleStringProperty(String.format("%02d/%d", data.getValue().getBillingMonth(), data.getValue().getBillingYear())));
        colMilkQty.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTotalMilkQuantity().toString() + " L"));
        colMilkChg.setCellValueFactory(data -> new SimpleStringProperty("₹" + data.getValue().getMilkCharges().toString()));
        colExtraChg.setCellValueFactory(data -> new SimpleStringProperty("₹" + data.getValue().getExtraOrdersCharges().toString()));
        colNet.setCellValueFactory(data -> new SimpleStringProperty("₹" + data.getValue().getNetPayable().toString()));
        colPaid.setCellValueFactory(data -> new SimpleStringProperty("₹" + data.getValue().getPaidAmount().toString()));
        colDue.setCellValueFactory(data -> new SimpleStringProperty("₹" + data.getValue().getBalanceDue().toString()));
        colStatus.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus().name()));
    }

    @FXML
    public void loadBills() {
        Customer c = SessionManager.getCurrentCustomer();
        Long cId = c != null ? c.getCustomerId() : 1L;

        List<MonthlyBill> list = billingService.getCustomerBillHistory(cId);
        billsTable.setItems(FXCollections.observableArrayList(list));
    }
}
