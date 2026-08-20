package com.agrodairy.management.controller;

import com.agrodairy.management.model.Customer;
import com.agrodairy.management.model.DeliveryRecord;
import com.agrodairy.management.model.ExtraOrder;
import com.agrodairy.management.model.MonthlyBill;
import com.agrodairy.management.model.Notification;
import com.agrodairy.management.model.UserLogin;
import com.agrodairy.management.service.BillingService;
import com.agrodairy.management.service.DeliveryService;
import com.agrodairy.management.service.ExtraOrderService;
import com.agrodairy.management.service.NotificationService;
import com.agrodairy.management.service.impl.BillingServiceImpl;
import com.agrodairy.management.service.impl.DeliveryServiceImpl;
import com.agrodairy.management.service.impl.ExtraOrderServiceImpl;
import com.agrodairy.management.service.impl.NotificationServiceImpl;
import com.agrodairy.management.util.SessionManager;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

public class CustomerDashboardController {

    @FXML private Label welcomeBannerLabel;
    @FXML private Label addressLabel;

    @FXML private Label deliveryStatusLabel;
    @FXML private Label subscriptionQtyLabel;
    @FXML private Label monthlyBillLabel;
    @FXML private Label paymentStatusLabel;
    @FXML private Label balanceDueLabel;
    @FXML private Label extraOrdersCountLabel;
    @FXML private Label unreadNotificationsLabel;

    private final DeliveryService deliveryService;
    private final BillingService billingService;
    private final ExtraOrderService extraOrderService;
    private final NotificationService notificationService;

    public CustomerDashboardController() {
        this.deliveryService = new DeliveryServiceImpl();
        this.billingService = new BillingServiceImpl();
        this.extraOrderService = new ExtraOrderServiceImpl();
        this.notificationService = new NotificationServiceImpl();
    }

    @FXML
    public void initialize() {
        refreshDashboard();
    }

    @FXML
    public void refreshDashboard() {
        Customer c = SessionManager.getCurrentCustomer();
        UserLogin u = SessionManager.getCurrentUser();

        if (c == null && u != null) {
            // Stub customer context if user login only
            c = new Customer();
            c.setCustomerId(1L);
            c.setFullName(u.getFullName());
            c.setAddress("Dharampeth, Nagpur");
        }

        if (c != null) {
            if (welcomeBannerLabel != null) welcomeBannerLabel.setText("Welcome back, " + c.getFullName());
            if (addressLabel != null) addressLabel.setText("Delivery Address: " + (c.getAddress() != null ? c.getAddress() : "Nagpur"));

            Long cId = c.getCustomerId() != null ? c.getCustomerId() : 1L;
            Date today = Date.valueOf(LocalDate.now());

            // 1. Today's Delivery Status
            List<DeliveryRecord> todayRecords = deliveryService.getDeliveriesByDate(today);
            DeliveryRecord record = todayRecords.stream()
                    .filter(r -> r.getCustomerId().equals(cId))
                    .findFirst().orElse(null);

            if (deliveryStatusLabel != null) {
                deliveryStatusLabel.setText(record != null ? record.getStatus().name() : "NOT DELIVERED YET");
            }
            if (subscriptionQtyLabel != null && c.getMorningQuantity() != null) {
                subscriptionQtyLabel.setText(String.format("Scheduled: M(%.1fL) E(%.1fL) %s", c.getMorningQuantity(), c.getEveningQuantity(), c.getMilkType() != null ? c.getMilkType().name() : "MILK"));
            }

            // 2. Monthly Bill & 3. Payment Status
            try {
                MonthlyBill bill = billingService.getCustomerMonthlyBill(cId, LocalDate.now().getMonthValue(), LocalDate.now().getYear());
                if (monthlyBillLabel != null) monthlyBillLabel.setText("₹" + bill.getNetPayable().toString());
                if (paymentStatusLabel != null) paymentStatusLabel.setText(bill.getStatus().name());
                if (balanceDueLabel != null) balanceDueLabel.setText("Balance Due: ₹" + bill.getBalanceDue().toString());
            } catch (Exception e) {
                if (monthlyBillLabel != null) monthlyBillLabel.setText("₹0.00");
                if (paymentStatusLabel != null) paymentStatusLabel.setText("NO BILL YET");
                if (balanceDueLabel != null) balanceDueLabel.setText("Balance Due: ₹0.00");
            }

            // 4. Extra Orders
            List<ExtraOrder> orders = extraOrderService.getCustomerOrders(cId);
            if (extraOrdersCountLabel != null) extraOrdersCountLabel.setText(orders.size() + " Orders");

            // 5. Unread Notifications
            List<Notification> unread = notificationService.getUnreadUserNotifications(u != null ? u.getUserId() : 1L);
            if (unreadNotificationsLabel != null) unreadNotificationsLabel.setText(unread.size() + " Unread");
        }
    }
}
