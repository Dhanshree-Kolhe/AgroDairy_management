package com.agrodairy.management.controller;

import com.agrodairy.management.dao.CustomerDAO;
import com.agrodairy.management.dao.impl.CustomerDAOImpl;
import com.agrodairy.management.model.Customer;
import com.agrodairy.management.model.MonthlyBill;
import com.agrodairy.management.model.enums.PaymentMode;
import com.agrodairy.management.service.BillingService;
import com.agrodairy.management.service.CustomerService;
import com.agrodairy.management.service.PaymentService;
import com.agrodairy.management.service.impl.BillingServiceImpl;
import com.agrodairy.management.service.impl.CustomerServiceImpl;
import com.agrodairy.management.service.impl.PaymentServiceImpl;
import com.agrodairy.management.util.AlertUtils;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;

import java.util.List;
import java.util.Optional;

public class BillingController {

    @FXML private ComboBox<Integer> monthComboBox;
    @FXML private TextField yearField;

    @FXML private TableView<MonthlyBill> billsTable;
    @FXML private TableColumn<MonthlyBill, String> colBillNum;
    @FXML private TableColumn<MonthlyBill, String> colCustName;
    @FXML private TableColumn<MonthlyBill, String> colPeriod;
    @FXML private TableColumn<MonthlyBill, String> colMilkQty;
    @FXML private TableColumn<MonthlyBill, String> colMilkChg;
    @FXML private TableColumn<MonthlyBill, String> colExtraChg;
    @FXML private TableColumn<MonthlyBill, String> colNet;
    @FXML private TableColumn<MonthlyBill, String> colPaid;
    @FXML private TableColumn<MonthlyBill, String> colDue;
    @FXML private TableColumn<MonthlyBill, String> colStatus;

    private final BillingService billingService;
    private final PaymentService paymentService;
    private final CustomerService customerService;
    private final CustomerDAO customerDAO;

    public BillingController() {
        this.billingService = new BillingServiceImpl();
        this.paymentService = new PaymentServiceImpl();
        this.customerService = new CustomerServiceImpl();
        this.customerDAO = new CustomerDAOImpl();
    }

    @FXML
    public void initialize() {
        if (monthComboBox != null) {
            monthComboBox.setItems(FXCollections.observableArrayList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12));
            monthComboBox.setValue(LocalDate.now().getMonthValue());
        }
        if (yearField != null) yearField.setText(String.valueOf(LocalDate.now().getYear()));

        setupTable();
        loadBillsForPeriod();
    }

    private void setupTable() {
        colBillNum.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getBillNumber()));
        colCustName.setCellValueFactory(data -> {
            Optional<Customer> c = customerDAO.findById(data.getValue().getCustomerId());
            return new SimpleStringProperty(c.isPresent() ? c.get().getFullName() : "Customer #" + data.getValue().getCustomerId());
        });
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
    public void loadBillsForPeriod() {
        try {
            int month = monthComboBox.getValue() != null ? monthComboBox.getValue() : LocalDate.now().getMonthValue();
            int year = Integer.parseInt(yearField.getText().trim());

            List<MonthlyBill> list = billingService.getMonthlyBillsByPeriod(month, year);
            billsTable.setItems(FXCollections.observableArrayList(list));
        } catch (Exception e) {
            AlertUtils.showError("Error", "Could not load monthly bills: " + e.getMessage());
        }
    }

    @FXML
    public void handleGenerateMonthlyBills() {
        try {
            int month = monthComboBox.getValue();
            int year = Integer.parseInt(yearField.getText().trim());

            List<Customer> customers = customerService.getAllCustomers();
            int generatedCount = 0;
            for (Customer c : customers) {
                if (c.getStatus() == com.agrodairy.management.model.enums.SubscriptionStatus.ACTIVE) {
                    billingService.generateMonthlyBill(c.getCustomerId(), month, year);
                    generatedCount++;
                }
            }

            AlertUtils.showInfo("Bills Generated", String.format("Successfully compiled %d monthly bills for period %02d/%d using snapshot rates.", generatedCount, month, year));
            loadBillsForPeriod();
        } catch (Exception e) {
            AlertUtils.showError("Billing Error", e.getMessage());
        }
    }

    @FXML
    public void handleOpenPaymentDialog() {
        MonthlyBill selectedBill = billsTable.getSelectionModel().getSelectedItem();
        if (selectedBill == null) {
            AlertUtils.showWarning("Selection Required", "Please select a bill from table to record payment.");
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Record Bill Payment");
        dialog.setHeaderText("Record Payment for Bill: " + selectedBill.getBillNumber());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField amountField = new TextField(selectedBill.getBalanceDue().toString());
        ComboBox<PaymentMode> modeCombo = new ComboBox<>(FXCollections.observableArrayList(PaymentMode.values()));
        modeCombo.setValue(PaymentMode.UPI);
        TextField refField = new TextField("UPI-" + System.currentTimeMillis() % 10000);

        grid.add(new Label("Balance Due:"), 0, 0);
        grid.add(new Label("₹" + selectedBill.getBalanceDue()), 1, 0);
        grid.add(new Label("Payment Amount (₹):"), 0, 1);
        grid.add(amountField, 1, 1);
        grid.add(new Label("Payment Mode:"), 0, 2);
        grid.add(modeCombo, 1, 2);
        grid.add(new Label("Transaction Ref:"), 0, 3);
        grid.add(refField, 1, 3);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                BigDecimal paidAmount = new BigDecimal(amountField.getText().trim());
                paymentService.recordPayment(selectedBill.getBillId(), selectedBill.getCustomerId(), paidAmount, new Date(System.currentTimeMillis()), modeCombo.getValue(), refField.getText().trim());
                AlertUtils.showInfo("Payment Recorded", "Payment of ₹" + paidAmount + " recorded successfully!");
                loadBillsForPeriod();
            } catch (Exception e) {
                AlertUtils.showError("Payment Failure", e.getMessage());
            }
        }
    }
}
