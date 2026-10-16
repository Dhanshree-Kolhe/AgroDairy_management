package com.agrodairy.management.controller;

import com.agrodairy.management.model.Customer;
import com.agrodairy.management.model.MonthlyBill;
import com.agrodairy.management.model.Payment;
import com.agrodairy.management.model.enums.PaymentStatus;
import com.agrodairy.management.service.BillingService;
import com.agrodairy.management.service.PaymentService;
import com.agrodairy.management.service.impl.BillingServiceImpl;
import com.agrodairy.management.service.impl.PaymentServiceImpl;
import com.agrodairy.management.util.AlertUtils;
import com.agrodairy.management.util.PdfInvoiceGenerator;
import com.agrodairy.management.util.SessionManager;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

public class CustomerBillsController {

    private static final Logger logger = LoggerFactory.getLogger(CustomerBillsController.class);

    @FXML private Label totalBilledLabel;
    @FXML private Label totalPaidLabel;
    @FXML private Label totalDueLabel;
    @FXML private Label dueStatusSubtext;
    @FXML private Label pendingCountLabel;
    @FXML private Label pendingBillsSubtext;

    @FXML private ComboBox<String> statusFilterCombo;
    @FXML private Button btnPaySelected;

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
    @FXML private TableColumn<MonthlyBill, MonthlyBill> colAction;

    private final BillingService billingService;
    private final PaymentService paymentService;

    private FilteredList<MonthlyBill> filteredBills;
    private final Set<Long> pendingBillIds = new HashSet<>();

    public CustomerBillsController() {
        this.billingService = new BillingServiceImpl();
        this.paymentService = new PaymentServiceImpl();
    }

    public CustomerBillsController(BillingService billingService) {
        this.billingService = billingService;
        this.paymentService = new PaymentServiceImpl();
    }

    public CustomerBillsController(BillingService billingService, PaymentService paymentService) {
        this.billingService = billingService;
        this.paymentService = paymentService;
    }

    @FXML
    public void initialize() {
        setupFilters();
        setupTable();
        loadBills();
    }

    private void setupFilters() {
        if (statusFilterCombo != null) {
            statusFilterCombo.setItems(FXCollections.observableArrayList("ALL", "UNPAID", "PARTIALLY PAID", "PAID"));
            statusFilterCombo.setValue("ALL");
            statusFilterCombo.valueProperty().addListener((obs, oldVal, newVal) -> applyFilter());
        }
    }

    private void setupTable() {
        colBillNum.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getBillNumber()));
        colPeriod.setCellValueFactory(data -> new SimpleStringProperty(String.format("%02d/%d", data.getValue().getBillingMonth(), data.getValue().getBillingYear())));
        colMilkQty.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTotalMilkQuantity() != null ? data.getValue().getTotalMilkQuantity().toString() + " L" : "0.0 L"));
        colMilkChg.setCellValueFactory(data -> new SimpleStringProperty("₹" + (data.getValue().getMilkCharges() != null ? data.getValue().getMilkCharges().toString() : "0.00")));
        colExtraChg.setCellValueFactory(data -> new SimpleStringProperty("₹" + (data.getValue().getExtraOrdersCharges() != null ? data.getValue().getExtraOrdersCharges().toString() : "0.00")));
        colNet.setCellValueFactory(data -> new SimpleStringProperty("₹" + (data.getValue().getNetPayable() != null ? data.getValue().getNetPayable().toString() : "0.00")));
        colPaid.setCellValueFactory(data -> new SimpleStringProperty("₹" + (data.getValue().getPaidAmount() != null ? data.getValue().getPaidAmount().toString() : "0.00")));
        colDue.setCellValueFactory(data -> new SimpleStringProperty("₹" + (data.getValue().getBalanceDue() != null ? data.getValue().getBalanceDue().toString() : "0.00")));

        // Status Column with consistent badges
        colStatus.setCellValueFactory(data -> new SimpleStringProperty(computeStatusLabel(data.getValue())));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label badge = new Label(status);
                    if (status.startsWith("PAID")) {
                        badge.setStyle("-fx-background-color: #ECFDF5; -fx-text-fill: #059669; -fx-padding: 4 9; -fx-background-radius: 6; -fx-font-weight: bold; -fx-font-size: 11px; -fx-border-color: #A7F3D0; -fx-border-radius: 6;");
                    } else if (status.contains("PARTIALLY") || status.contains("Verification")) {
                        badge.setStyle("-fx-background-color: #FFFBEB; -fx-text-fill: #D97706; -fx-padding: 4 9; -fx-background-radius: 6; -fx-font-weight: bold; -fx-font-size: 11px; -fx-border-color: #FDE68A; -fx-border-radius: 6;");
                    } else {
                        badge.setStyle("-fx-background-color: #FEF2F2; -fx-text-fill: #DC2626; -fx-padding: 4 9; -fx-background-radius: 6; -fx-font-weight: bold; -fx-font-size: 11px; -fx-border-color: #FECACA; -fx-border-radius: 6;");
                    }
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        // Payment Action Column: Pay Now, Awaiting Verification, or Settled
        colAction.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue()));
        colAction.setCellFactory(col -> new TableCell<>() {
            private final Button payBtn = new Button("💳 Pay Now");
            private final Label settledBadge = new Label("✅ Settled");
            private final Label awaitingBadge = new Label("⏳ Awaiting Verification");

            {
                payBtn.getStyleClass().add("btn-sm");
                payBtn.setStyle("-fx-background-color: #059669; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 4 10; -fx-background-radius: 6; -fx-cursor: hand;");
                settledBadge.setStyle("-fx-background-color: #ECFDF5; -fx-text-fill: #059669; -fx-padding: 4 9; -fx-background-radius: 6; -fx-font-weight: bold; -fx-font-size: 11.5px; -fx-border-color: #A7F3D0; -fx-border-radius: 6;");
                awaitingBadge.setStyle("-fx-background-color: #FEF3C7; -fx-text-fill: #92400E; -fx-padding: 4 8; -fx-background-radius: 6; -fx-font-weight: bold; -fx-font-size: 11px; -fx-border-color: #FDE68A; -fx-border-radius: 6;");
                awaitingBadge.setTooltip(new Tooltip("A payment has been submitted and is awaiting vendor/admin verification."));
            }

            @Override
            protected void updateItem(MonthlyBill bill, boolean empty) {
                super.updateItem(bill, empty);
                if (empty || bill == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    boolean isSettled = isBillSettled(bill);
                    boolean hasPending = pendingBillIds.contains(bill.getBillId());

                    if (isSettled) {
                        setGraphic(settledBadge);
                    } else if (hasPending) {
                        setGraphic(awaitingBadge);
                    } else {
                        payBtn.setOnAction(event -> openPaymentDialog(bill));
                        setGraphic(payBtn);
                    }
                    setText(null);
                }
            }
        });
    }

    private String computeStatusLabel(MonthlyBill bill) {
        if (bill == null) return "UNPAID";
        boolean isSettled = isBillSettled(bill);
        boolean hasPending = pendingBillIds.contains(bill.getBillId());

        if (isSettled) {
            return "PAID";
        }
        if (hasPending) {
            if (bill.getStatus() == PaymentStatus.PARTIAL || (bill.getPaidAmount() != null && bill.getPaidAmount().compareTo(BigDecimal.ZERO) > 0)) {
                return "PARTIALLY PAID (Pending Verification)";
            }
            return "UNPAID (Pending Verification)";
        }
        if (bill.getStatus() == PaymentStatus.PARTIAL || (bill.getPaidAmount() != null && bill.getPaidAmount().compareTo(BigDecimal.ZERO) > 0)) {
            return "PARTIALLY PAID";
        }
        return "UNPAID";
    }

    private boolean isBillSettled(MonthlyBill bill) {
        if (bill == null) return true;
        if (bill.getStatus() == PaymentStatus.PAID) return true;
        if (bill.getBalanceDue() != null && bill.getBalanceDue().compareTo(BigDecimal.ZERO) <= 0) return true;
        return bill.getNetPayable() != null && bill.getNetPayable().compareTo(BigDecimal.ZERO) <= 0;
    }

    @FXML
    public void loadBills() {
        Customer c = SessionManager.getCurrentCustomer();
        Long cId = c != null ? c.getCustomerId() : 1L;

        // 1. Fetch pending payment records for this customer
        pendingBillIds.clear();
        try {
            List<Payment> customerPayments = paymentService.getPaymentsByCustomer(cId);
            for (Payment p : customerPayments) {
                if ("PENDING".equalsIgnoreCase(p.getStatus())) {
                    pendingBillIds.add(p.getBillId());
                }
            }
        } catch (Exception e) {
            logger.warn("Could not query pending payments for customer {}: {}", cId, e.getMessage());
        }

        // 2. Fetch reconciled bills
        List<MonthlyBill> list = billingService.getCustomerBillHistory(cId);
        filteredBills = new FilteredList<>(FXCollections.observableArrayList(list), b -> true);
        billsTable.setItems(filteredBills);

        updateSummaryMetrics(list);
        applyFilter();
    }

    private void updateSummaryMetrics(List<MonthlyBill> bills) {
        BigDecimal totalBilled = bills.stream()
                .map(b -> b.getNetPayable() != null ? b.getNetPayable() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalPaid = bills.stream()
                .map(b -> b.getPaidAmount() != null ? b.getPaidAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalDue = bills.stream()
                .map(b -> b.getBalanceDue() != null ? b.getBalanceDue() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        long pendingCount = bills.stream()
                .filter(b -> b.getBalanceDue() != null && b.getBalanceDue().compareTo(BigDecimal.ZERO) > 0)
                .count();

        if (totalBilledLabel != null) totalBilledLabel.setText("₹" + totalBilled);
        if (totalPaidLabel != null) totalPaidLabel.setText("₹" + totalPaid);
        if (totalDueLabel != null) totalDueLabel.setText("₹" + totalDue);

        if (dueStatusSubtext != null) {
            if (totalDue.compareTo(BigDecimal.ZERO) == 0) {
                dueStatusSubtext.setText("All accounts settled ✓");
                dueStatusSubtext.setStyle("-fx-text-fill: #059669; -fx-font-weight: bold; -fx-font-size: 11.5px;");
            } else {
                dueStatusSubtext.setText("Clearance required (" + pendingCount + " pending)");
                dueStatusSubtext.setStyle("-fx-text-fill: #DC2626; -fx-font-weight: bold; -fx-font-size: 11.5px;");
            }
        }

        if (pendingCountLabel != null) pendingCountLabel.setText(bills.size() + " Statements");
        if (pendingBillsSubtext != null) {
            pendingBillsSubtext.setText(pendingCount + " bills pending payment");
        }
    }

    private void applyFilter() {
        if (filteredBills == null) return;
        String filter = statusFilterCombo != null ? statusFilterCombo.getValue() : "ALL";

        filteredBills.setPredicate(bill -> {
            if (filter == null || "ALL".equalsIgnoreCase(filter)) return true;
            if (bill.getStatus() == null) return false;

            if ("PARTIALLY PAID".equalsIgnoreCase(filter) || "PARTIAL".equalsIgnoreCase(filter)) {
                return bill.getStatus() == PaymentStatus.PARTIAL;
            }
            if ("PAID".equalsIgnoreCase(filter)) {
                return bill.getStatus() == PaymentStatus.PAID;
            }
            if ("UNPAID".equalsIgnoreCase(filter)) {
                return bill.getStatus() == PaymentStatus.UNPAID;
            }
            return bill.getStatus().name().equalsIgnoreCase(filter);
        });
    }

    @FXML
    public void handlePaySelectedBill() {
        MonthlyBill selected = billsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            // If no row selected, check if there is an eligible unpaid/partial bill
            if (filteredBills != null) {
                for (MonthlyBill b : filteredBills) {
                    if (b.getBalanceDue() != null && b.getBalanceDue().compareTo(BigDecimal.ZERO) > 0
                            && !pendingBillIds.contains(b.getBillId())) {
                        openPaymentDialog(b);
                        return;
                    }
                }
            }
            AlertUtils.showWarning("Selection Required", "Please select an unpaid or partially paid bill from the table to settle payment.");
            return;
        }

        if (isBillSettled(selected)) {
            AlertUtils.showInfo("Already Settled", "The selected bill (" + selected.getBillNumber() + ") has already been settled in full. No payment is required.");
            return;
        }

        if (pendingBillIds.contains(selected.getBillId())) {
            AlertUtils.showInfo("Payment Pending Verification", "A payment for bill (" + selected.getBillNumber() + ") has already been submitted and is currently awaiting vendor/admin verification. Duplicate payments are not permitted.");
            return;
        }

        openPaymentDialog(selected);
    }

    public void openPaymentDialog(MonthlyBill bill) {
        if (bill == null) return;

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/customer/CustomerPaymentDialog.fxml"));
            Parent root = loader.load();

            CustomerPaymentDialogController controller = loader.getController();

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Pay Bill — " + bill.getBillNumber());
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            if (billsTable != null && billsTable.getScene() != null) {
                dialogStage.initOwner(billsTable.getScene().getWindow());
            }

            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());
            dialogStage.setScene(scene);
            dialogStage.setResizable(false);

            Customer customer = SessionManager.getCurrentCustomer();
            controller.setBillData(bill, customer, dialogStage, this::loadBills);

            dialogStage.showAndWait();
        } catch (Exception e) {
            logger.error("Failed to open payment dialog", e);
            AlertUtils.showError("Dialog Error", "Could not open payment dialog: " + e.getMessage());
        }
    }

    @FXML
    public void handleViewPaymentHistory() {
        MonthlyBill selectedBill = billsTable.getSelectionModel().getSelectedItem();
        Customer customer = SessionManager.getCurrentCustomer();
        Long cId = customer != null ? customer.getCustomerId() : 1L;

        List<Payment> payments;
        String title;
        if (selectedBill != null) {
            payments = paymentService.getPaymentsByBill(selectedBill.getBillId());
            title = "Payment History — Bill " + selectedBill.getBillNumber();
        } else {
            payments = paymentService.getPaymentsByCustomer(cId);
            title = "Payment History — All Statements";
        }

        showPaymentHistoryDialog(title, payments);
    }

    private void showPaymentHistoryDialog(String title, List<Payment> payments) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText("Payment Records & Verified Transactions");
        if (billsTable != null && billsTable.getScene() != null) {
            dialog.initOwner(billsTable.getScene().getWindow());
        }

        VBox content = new VBox(10);
        content.setPrefWidth(560);
        content.setPrefHeight(320);

        if (payments == null || payments.isEmpty()) {
            Label emptyLabel = new Label("No payment records found for this selection.");
            emptyLabel.setStyle("-fx-text-fill: #64748B; -fx-font-size: 13px; -fx-padding: 20;");
            content.getChildren().add(emptyLabel);
        } else {
            TableView<Payment> pTable = new TableView<>(FXCollections.observableArrayList(payments));
            pTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

            TableColumn<Payment, String> colRef = new TableColumn<>("Reference / UTR");
            colRef.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getTransactionReference() != null ? p.getValue().getTransactionReference() : "N/A"));

            TableColumn<Payment, String> colDate = new TableColumn<>("Date");
            colDate.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getPaymentDate() != null ? p.getValue().getPaymentDate().toString() : ""));

            TableColumn<Payment, String> colMode = new TableColumn<>("Mode");
            colMode.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getPaymentMode() != null ? p.getValue().getPaymentMode().name() : ""));

            TableColumn<Payment, String> colAmt = new TableColumn<>("Amount");
            colAmt.setCellValueFactory(p -> new SimpleStringProperty("₹" + (p.getValue().getAmountPaid() != null ? p.getValue().getAmountPaid().toString() : "0.00")));

            TableColumn<Payment, String> colSt = new TableColumn<>("Status");
            colSt.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getStatus() != null ? p.getValue().getStatus() : "SUCCESS"));
            colSt.setCellFactory(col -> new TableCell<>() {
                @Override
                protected void updateItem(String status, boolean empty) {
                    super.updateItem(status, empty);
                    if (empty || status == null) {
                        setGraphic(null);
                        setText(null);
                    } else {
                        Label badge = new Label(status);
                        if ("SUCCESS".equalsIgnoreCase(status)) {
                            badge.setStyle("-fx-background-color: #ECFDF5; -fx-text-fill: #059669; -fx-padding: 3 8; -fx-background-radius: 6; -fx-font-weight: bold; -fx-font-size: 11px;");
                        } else if ("PENDING".equalsIgnoreCase(status)) {
                            badge.setStyle("-fx-background-color: #FEF3C7; -fx-text-fill: #92400E; -fx-padding: 3 8; -fx-background-radius: 6; -fx-font-weight: bold; -fx-font-size: 11px;");
                        } else {
                            badge.setStyle("-fx-background-color: #FEF2F2; -fx-text-fill: #DC2626; -fx-padding: 3 8; -fx-background-radius: 6; -fx-font-weight: bold; -fx-font-size: 11px;");
                        }
                        setGraphic(badge);
                        setText(null);
                    }
                }
            });

            pTable.getColumns().addAll(colRef, colDate, colMode, colAmt, colSt);
            content.getChildren().add(pTable);
        }

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    @FXML
    public void handleExportPdfInvoice() {
        MonthlyBill selectedBill = billsTable.getSelectionModel().getSelectedItem();
        if (selectedBill == null) {
            AlertUtils.showWarning("Selection Required", "Please select a bill to download invoice PDF.");
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
            List<Payment> payments = paymentService.getPaymentsByBill(selectedBill.getBillId());

            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Download Monthly Invoice PDF");
            fileChooser.setInitialFileName(selectedBill.getBillNumber() + ".pdf");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Documents (*.pdf)", "*.pdf"));

            File file = fileChooser.showSaveDialog(billsTable.getScene().getWindow());
            if (file != null) {
                PdfInvoiceGenerator.generateMonthlyInvoicePdf(selectedBill, customer, delRecords, extraOrders, payments, file);
                AlertUtils.showInfo("Invoice Downloaded", "Invoice PDF saved successfully to:\n" + file.getAbsolutePath());
            }
        } catch (Exception e) {
            AlertUtils.showError("Download Error", "Failed to download PDF invoice: " + e.getMessage());
        }
    }
}
