package com.agrodairy.management.controller;

import com.agrodairy.management.model.Customer;
import com.agrodairy.management.model.DeliveryRecord;
import com.agrodairy.management.model.ExtraOrder;
import com.agrodairy.management.model.MonthlyBill;
import com.agrodairy.management.model.Notification;
import com.agrodairy.management.model.UserLogin;
import com.agrodairy.management.model.enums.DeliveryStatus;
import com.agrodairy.management.model.enums.PaymentStatus;
import com.agrodairy.management.service.BillingService;
import com.agrodairy.management.service.DeliveryService;
import com.agrodairy.management.service.ExtraOrderService;
import com.agrodairy.management.service.NotificationService;
import com.agrodairy.management.service.impl.BillingServiceImpl;
import com.agrodairy.management.service.impl.DeliveryServiceImpl;
import com.agrodairy.management.service.impl.ExtraOrderServiceImpl;
import com.agrodairy.management.service.impl.NotificationServiceImpl;
import com.agrodairy.management.util.AlertUtils;
import com.agrodairy.management.util.NavigationManager;
import com.agrodairy.management.util.SessionManager;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

public class CustomerDashboardController {

    private static final Logger logger = LoggerFactory.getLogger(CustomerDashboardController.class);

    @FXML private Label welcomeBannerLabel;
    @FXML private Label planStatusBadge;
    @FXML private Label addressLabel;

    @FXML private Label deliveryStatusLabel;
    @FXML private Label subscriptionQtyLabel;
    @FXML private Label monthlyBillLabel;
    @FXML private Label billingPeriodSubtext;
    @FXML private Label paymentStatusLabel;
    @FXML private Label balanceDueLabel;
    @FXML private Label dueStatusNotice;
    @FXML private Button btnQuickPay;
    @FXML private Label dailyQuotaLabel;
    @FXML private Label milkRateSubtext;
    @FXML private Label extraOrdersCountLabel;
    @FXML private Label unreadNotificationsLabel;

    // Recent Deliveries Table
    @FXML private TableView<DeliveryRecord> recentDeliveriesTable;
    @FXML private TableColumn<DeliveryRecord, String> colDelDate;
    @FXML private TableColumn<DeliveryRecord, String> colDelShift;
    @FXML private TableColumn<DeliveryRecord, String> colDelQty;
    @FXML private TableColumn<DeliveryRecord, String> colDelTotal;
    @FXML private TableColumn<DeliveryRecord, String> colDelStatus;

    // Recent Bills Table
    @FXML private TableView<MonthlyBill> recentBillsTable;
    @FXML private TableColumn<MonthlyBill, String> colDashBillNum;
    @FXML private TableColumn<MonthlyBill, String> colDashBillPeriod;
    @FXML private TableColumn<MonthlyBill, String> colDashBillNet;
    @FXML private TableColumn<MonthlyBill, String> colDashBillDue;
    @FXML private TableColumn<MonthlyBill, String> colDashBillStatus;
    @FXML private TableColumn<MonthlyBill, Void> colDashBillAction;

    private final DeliveryService deliveryService;
    private final BillingService billingService;
    private final ExtraOrderService extraOrderService;
    private final NotificationService notificationService;

    private MonthlyBill latestPendingBill;

    public CustomerDashboardController() {
        this.deliveryService = new DeliveryServiceImpl();
        this.billingService = new BillingServiceImpl();
        this.extraOrderService = new ExtraOrderServiceImpl();
        this.notificationService = new NotificationServiceImpl();
    }

    public CustomerDashboardController(DeliveryService deliveryService, BillingService billingService,
                                       ExtraOrderService extraOrderService, NotificationService notificationService) {
        this.deliveryService = deliveryService;
        this.billingService = billingService;
        this.extraOrderService = extraOrderService;
        this.notificationService = notificationService;
    }

    @FXML
    public void initialize() {
        setupTables();
        refreshDashboard();
    }

    private void setupTables() {
        // Setup Deliveries Table
        if (colDelDate != null) colDelDate.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDeliveryDate() != null ? d.getValue().getDeliveryDate().toString() : ""));
        if (colDelShift != null) colDelShift.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDeliveryShift() != null ? d.getValue().getDeliveryShift().name() : ""));
        if (colDelQty != null) colDelQty.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDeliveredQuantity() != null ? d.getValue().getDeliveredQuantity() + " L" : "0.0 L"));
        if (colDelTotal != null) colDelTotal.setCellValueFactory(d -> new SimpleStringProperty("₹" + (d.getValue().getTotalAmount() != null ? d.getValue().getTotalAmount() : "0.00")));
        if (colDelStatus != null) {
            colDelStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus() != null ? d.getValue().getStatus().name() : ""));
            colDelStatus.setCellFactory(col -> new TableCell<>() {
                @Override
                protected void updateItem(String status, boolean empty) {
                    super.updateItem(status, empty);
                    if (empty || status == null) {
                        setGraphic(null);
                        setText(null);
                    } else {
                        Label badge = new Label(status);
                        if ("DELIVERED".equalsIgnoreCase(status)) {
                            badge.getStyleClass().add("badge-success");
                        } else if ("PAUSED".equalsIgnoreCase(status)) {
                            badge.getStyleClass().add("badge-warning");
                        } else {
                            badge.getStyleClass().add("badge-danger");
                        }
                        setGraphic(badge);
                        setText(null);
                    }
                }
            });
        }

        // Setup Recent Bills Table
        if (colDashBillNum != null) colDashBillNum.setCellValueFactory(b -> new SimpleStringProperty(b.getValue().getBillNumber()));
        if (colDashBillPeriod != null) colDashBillPeriod.setCellValueFactory(b -> new SimpleStringProperty(String.format("%02d/%d", b.getValue().getBillingMonth(), b.getValue().getBillingYear())));
        if (colDashBillNet != null) colDashBillNet.setCellValueFactory(b -> new SimpleStringProperty("₹" + (b.getValue().getNetPayable() != null ? b.getValue().getNetPayable() : "0.00")));
        if (colDashBillDue != null) colDashBillDue.setCellValueFactory(b -> new SimpleStringProperty("₹" + (b.getValue().getBalanceDue() != null ? b.getValue().getBalanceDue() : "0.00")));
        if (colDashBillStatus != null) {
            colDashBillStatus.setCellValueFactory(b -> new SimpleStringProperty(b.getValue().getStatus() != null ? b.getValue().getStatus().name() : ""));
            colDashBillStatus.setCellFactory(col -> new TableCell<>() {
                @Override
                protected void updateItem(String status, boolean empty) {
                    super.updateItem(status, empty);
                    if (empty || status == null) {
                        setGraphic(null);
                        setText(null);
                    } else {
                        Label badge = new Label(status);
                        if ("PAID".equalsIgnoreCase(status)) {
                            badge.getStyleClass().add("badge-success");
                        } else if ("PARTIAL".equalsIgnoreCase(status)) {
                            badge.getStyleClass().add("badge-warning");
                        } else {
                            badge.getStyleClass().add("badge-danger");
                        }
                        setGraphic(badge);
                        setText(null);
                    }
                }
            });
        }

        if (colDashBillAction != null) {
            colDashBillAction.setCellFactory(col -> new TableCell<>() {
                private final Button payBtn = new Button("Pay");
                private final Label settledLabel = new Label("✓");

                {
                    payBtn.getStyleClass().add("btn-sm");
                    payBtn.setStyle("-fx-background-color: #059669; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 3 8; -fx-background-radius: 4; -fx-font-size: 11px;");
                    settledLabel.setStyle("-fx-text-fill: #059669; -fx-font-weight: bold; -fx-font-size: 13px;");
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                        setGraphic(null);
                    } else {
                        MonthlyBill bill = (MonthlyBill) getTableRow().getItem();
                        boolean isSettled = bill.getStatus() == PaymentStatus.PAID
                                || (bill.getBalanceDue() != null && bill.getBalanceDue().compareTo(BigDecimal.ZERO) <= 0)
                                || (bill.getNetPayable() != null && bill.getNetPayable().compareTo(BigDecimal.ZERO) <= 0);

                        if (!isSettled) {
                            payBtn.setOnAction(event -> openPaymentDialog(bill));
                            setGraphic(payBtn);
                        } else {
                            setGraphic(settledLabel);
                        }
                    }
                }
            });
        }
    }

    @FXML
    public void refreshDashboard() {
        Customer c = SessionManager.getCurrentCustomer();
        UserLogin u = SessionManager.getCurrentUser();

        if (c == null && u != null) {
            c = new Customer();
            c.setCustomerId(1L);
            c.setFullName(u.getFullName());
            c.setAddress("Dharampeth, Nagpur");
        }

        if (c != null) {
            if (welcomeBannerLabel != null) welcomeBannerLabel.setText("Welcome back, " + c.getFullName());
            if (addressLabel != null) {
                String addr = c.getAddress() != null ? c.getAddress() : (c.getArea() != null ? c.getArea() + ", Nagpur" : "Nagpur");
                String phone = c.getPhone() != null ? " • Phone: " + c.getPhone() : "";
                addressLabel.setText("📍 Delivery Address: " + addr + phone);
            }

            Long cId = c.getCustomerId() != null ? c.getCustomerId() : 1L;
            Date today = Date.valueOf(LocalDate.now());

            // 1. Today's Delivery Status
            List<DeliveryRecord> todayRecords = deliveryService.getDeliveriesByDate(today);
            DeliveryRecord record = todayRecords.stream()
                    .filter(r -> r.getCustomerId().equals(cId))
                    .findFirst().orElse(null);

            if (deliveryStatusLabel != null) {
                if (record != null) {
                    deliveryStatusLabel.setText(record.getStatus().name());
                } else {
                    deliveryStatusLabel.setText("SCHEDULED");
                }
            }

            BigDecimal morningQty = c.getMorningQuantity() != null ? c.getMorningQuantity() : BigDecimal.ZERO;
            BigDecimal eveningQty = c.getEveningQuantity() != null ? c.getEveningQuantity() : BigDecimal.ZERO;
            BigDecimal totalDaily = morningQty.add(eveningQty);

            if (subscriptionQtyLabel != null) {
                subscriptionQtyLabel.setText(String.format("Scheduled: M(%.1fL) E(%.1fL)", morningQty, eveningQty));
            }
            if (dailyQuotaLabel != null) {
                dailyQuotaLabel.setText(totalDaily + " L / Day");
            }
            if (milkRateSubtext != null) {
                String milkType = c.getMilkType() != null ? c.getMilkType().name() : "MILK";
                BigDecimal rate = c.getCurrentPricePerLiter() != null ? c.getCurrentPricePerLiter() : new BigDecimal("60.00");
                milkRateSubtext.setText("Pure " + milkType + " @ ₹" + rate + "/L");
            }

            // 2. Monthly Bills & Outstanding Balance Calculation
            List<MonthlyBill> bills = billingService.getCustomerBillHistory(cId);
            BigDecimal totalOutstanding = bills.stream()
                    .map(b -> b.getBalanceDue() != null ? b.getBalanceDue() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_UP);

            latestPendingBill = bills.stream()
                    .filter(b -> b.getBalanceDue() != null && b.getBalanceDue().compareTo(BigDecimal.ZERO) > 0)
                    .findFirst().orElse(null);

            MonthlyBill latestBill = !bills.isEmpty() ? bills.get(0) : null;

            if (monthlyBillLabel != null) {
                monthlyBillLabel.setText("₹" + (latestBill != null && latestBill.getNetPayable() != null ? latestBill.getNetPayable() : "0.00"));
            }
            if (billingPeriodSubtext != null) {
                if (latestBill != null) {
                    billingPeriodSubtext.setText(String.format("Statement %02d/%d", latestBill.getBillingMonth(), latestBill.getBillingYear()));
                } else {
                    billingPeriodSubtext.setText("No bills generated yet");
                }
            }

            if (balanceDueLabel != null) {
                balanceDueLabel.setText("₹" + totalOutstanding);
            }

            if (paymentStatusLabel != null) {
                if (totalOutstanding.compareTo(BigDecimal.ZERO) == 0) {
                    paymentStatusLabel.setText("PAID");
                    paymentStatusLabel.setStyle("-fx-background-color: #ECFDF5; -fx-text-fill: #059669; -fx-padding: 3 8; -fx-background-radius: 12; -fx-font-size: 11px; -fx-font-weight: bold;");
                } else if (latestPendingBill != null && latestPendingBill.getStatus() == PaymentStatus.PARTIAL) {
                    paymentStatusLabel.setText("PARTIAL");
                    paymentStatusLabel.setStyle("-fx-background-color: #FFFBEB; -fx-text-fill: #D97706; -fx-padding: 3 8; -fx-background-radius: 12; -fx-font-size: 11px; -fx-font-weight: bold;");
                } else {
                    paymentStatusLabel.setText("DUE NOW");
                    paymentStatusLabel.setStyle("-fx-background-color: #FEF2F2; -fx-text-fill: #DC2626; -fx-padding: 3 8; -fx-background-radius: 12; -fx-font-size: 11px; -fx-font-weight: bold;");
                }
            }

            if (dueStatusNotice != null) {
                if (totalOutstanding.compareTo(BigDecimal.ZERO) == 0) {
                    dueStatusNotice.setText("All accounts settled ✓");
                    dueStatusNotice.setStyle("-fx-text-fill: #059669; -fx-font-weight: bold; -fx-font-size: 11.5px;");
                } else {
                    dueStatusNotice.setText("Payment required");
                    dueStatusNotice.setStyle("-fx-text-fill: #DC2626; -fx-font-weight: bold; -fx-font-size: 11.5px;");
                }
            }

            if (btnQuickPay != null) {
                btnQuickPay.setVisible(totalOutstanding.compareTo(BigDecimal.ZERO) > 0);
                btnQuickPay.setManaged(totalOutstanding.compareTo(BigDecimal.ZERO) > 0);
            }

            // 4. Extra Orders
            List<ExtraOrder> orders = extraOrderService.getCustomerOrders(cId);
            if (extraOrdersCountLabel != null) extraOrdersCountLabel.setText(orders.size() + " Orders");

            // 5. Unread Notifications
            List<Notification> unread = notificationService.getUnreadUserNotifications(u != null ? u.getUserId() : 1L);
            if (unreadNotificationsLabel != null) unreadNotificationsLabel.setText(unread.size() + " Alerts");

            // Populate Recent Deliveries Table
            List<DeliveryRecord> recentDelList = deliveryService.getCustomerDeliveryHistory(cId, LocalDate.now().getMonthValue(), LocalDate.now().getYear());
            if (recentDelList.isEmpty()) {
                // If current month has no history, fetch August 2026 or all available
                recentDelList = deliveryService.getCustomerDeliveryHistory(cId, 8, 2026);
            }
            if (recentDeliveriesTable != null) {
                recentDeliveriesTable.setItems(FXCollections.observableArrayList(recentDelList));
            }

            // Populate Recent Bills Table
            if (recentBillsTable != null) {
                recentBillsTable.setItems(FXCollections.observableArrayList(bills));
            }
        }
    }

    @FXML
    public void handleQuickPay() {
        if (latestPendingBill != null) {
            openPaymentDialog(latestPendingBill);
        } else {
            // Check if any bill in customer's history has due balance
            Customer c = SessionManager.getCurrentCustomer();
            Long cId = c != null ? c.getCustomerId() : 1L;
            List<MonthlyBill> bills = billingService.getCustomerBillHistory(cId);
            MonthlyBill dueBill = bills.stream()
                    .filter(b -> b.getBalanceDue() != null && b.getBalanceDue().compareTo(BigDecimal.ZERO) > 0)
                    .findFirst().orElse(null);

            if (dueBill != null) {
                openPaymentDialog(dueBill);
            } else {
                AlertUtils.showInfo("No Outstanding Dues", "All your bills are completely settled! No balance is due.");
            }
        }
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
            if (welcomeBannerLabel != null && welcomeBannerLabel.getScene() != null) {
                dialogStage.initOwner(welcomeBannerLabel.getScene().getWindow());
            }

            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());
            dialogStage.setScene(scene);
            dialogStage.setResizable(false);

            Customer customer = SessionManager.getCurrentCustomer();
            controller.setBillData(bill, customer, dialogStage, this::refreshDashboard);

            dialogStage.showAndWait();
        } catch (Exception e) {
            logger.error("Failed to open payment dialog", e);
            AlertUtils.showError("Dialog Error", "Could not open payment dialog: " + e.getMessage());
        }
    }

    @FXML
    public void handleNavBills() {
        NavigationManager.loadCenterView("/fxml/customer/CustomerBillsView.fxml");
    }

    @FXML
    public void handleNavDeliveries() {
        NavigationManager.loadCenterView("/fxml/customer/CustomerDeliveriesView.fxml");
    }

    @FXML
    public void handleNavExtraOrders() {
        NavigationManager.loadCenterView("/fxml/customer/CustomerExtraOrdersView.fxml");
    }

    @FXML
    public void handleNavHoliday() {
        NavigationManager.loadCenterView("/fxml/customer/CustomerHolidayView.fxml");
    }

    @FXML
    public void handleNavKnowledge() {
        NavigationManager.loadCenterView("/fxml/common/KnowledgeAssistantView.fxml");
    }
}
