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

    @FXML
    public void handleExportPdfInvoice() {
        MonthlyBill selectedBill = billsTable.getSelectionModel().getSelectedItem();
        if (selectedBill == null) {
            com.agrodairy.management.util.AlertUtils.showWarning("Selection Required", "Please select a bill to download invoice PDF.");
            return;
        }

        try {
            Customer customer = SessionManager.getCurrentCustomer();
            if (customer == null) {
                customer = new com.agrodairy.management.dao.impl.CustomerDAOImpl().findById(selectedBill.getCustomerId()).orElse(new Customer());
            }

            List<com.agrodairy.management.model.DeliveryRecord> delRecords = new com.agrodairy.management.dao.impl.DeliveryRecordDAOImpl()
                    .findByCustomerAndMonth(selectedBill.getCustomerId(), selectedBill.getBillingMonth(), selectedBill.getBillingYear());
            List<com.agrodairy.management.model.ExtraOrder> extraOrders = new com.agrodairy.management.dao.impl.ExtraOrderDAOImpl()
                    .findByCustomerAndMonth(selectedBill.getCustomerId(), selectedBill.getBillingMonth(), selectedBill.getBillingYear());
            List<com.agrodairy.management.model.Payment> payments = new com.agrodairy.management.dao.impl.PaymentDAOImpl()
                    .findByBillId(selectedBill.getBillId());

            javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
            fileChooser.setTitle("Download Monthly Invoice PDF");
            fileChooser.setInitialFileName(selectedBill.getBillNumber() + ".pdf");
            fileChooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("PDF Documents (*.pdf)", "*.pdf"));

            java.io.File file = fileChooser.showSaveDialog(billsTable.getScene().getWindow());
            if (file != null) {
                com.agrodairy.management.util.PdfInvoiceGenerator.generateMonthlyInvoicePdf(selectedBill, customer, delRecords, extraOrders, payments, file);
                com.agrodairy.management.util.AlertUtils.showInfo("Invoice Downloaded", "Invoice PDF saved successfully to:\n" + file.getAbsolutePath());
            }
        } catch (Exception e) {
            com.agrodairy.management.util.AlertUtils.showError("Download Error", "Failed to download PDF invoice: " + e.getMessage());
        }
    }
}
