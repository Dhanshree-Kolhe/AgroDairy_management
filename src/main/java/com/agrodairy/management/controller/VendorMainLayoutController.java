package com.agrodairy.management.controller;

import com.agrodairy.management.model.UserLogin;
import com.agrodairy.management.util.NavigationManager;
import com.agrodairy.management.util.SessionManager;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;

public class VendorMainLayoutController {

    @FXML private BorderPane mainBorderPane;
    @FXML private Label pageTitleLabel;
    @FXML private Label userWelcomeLabel;

    @FXML private Button navDashboardBtn;
    @FXML private Button navCustomersBtn;
    @FXML private Button navDeliveriesBtn;
    @FXML private Button navBillingBtn;
    @FXML private Button navInventoryBtn;
    @FXML private Button navExtraOrdersBtn;
    @FXML private Button navHolidaysBtn;
    @FXML private Button navRoutePlannerBtn;
    @FXML private Button navNotificationsBtn;
    @FXML private Button navKnowledgeBtn;

    private Button currentActiveBtn;

    @FXML
    public void initialize() {
        if (!SessionManager.isVendor()) {
            com.agrodairy.management.util.AlertUtils.showError("Access Denied", "Vendor privileges are required to access this portal.");
            handleLogout();
            return;
        }

        NavigationManager.setMainLayoutPane(mainBorderPane);
        UserLogin user = SessionManager.getCurrentUser();
        if (user != null && userWelcomeLabel != null) {
            userWelcomeLabel.setText("Welcome, " + user.getFullName());
        }
        handleNavDashboard();
    }

    @FXML
    public void handleNavDashboard() {
        setActiveNav(navDashboardBtn, "Vendor Overview Dashboard");
        NavigationManager.loadCenterView("/fxml/vendor/VendorDashboardView.fxml");
    }

    @FXML
    public void handleNavCustomers() {
        setActiveNav(navCustomersBtn, "Customer Directory & Management");
        NavigationManager.loadCenterView("/fxml/vendor/CustomerManagementView.fxml");
    }

    @FXML
    public void handleNavDeliveries() {
        setActiveNav(navDeliveriesBtn, "Daily Shift Milk Deliveries");
        NavigationManager.loadCenterView("/fxml/vendor/DailyDeliveriesView.fxml");
    }

    @FXML
    public void handleNavBilling() {
        setActiveNav(navBillingBtn, "Monthly Billing & Payment Collection");
        NavigationManager.loadCenterView("/fxml/vendor/BillingView.fxml");
    }

    @FXML
    public void handleNavInventory() {
        setActiveNav(navInventoryBtn, "Dairy Inventory & Product Stock");
        NavigationManager.loadCenterView("/fxml/vendor/InventoryView.fxml");
    }

    @FXML
    public void handleNavExtraOrders() {
        setActiveNav(navExtraOrdersBtn, "Customer Extra Product Orders");
        NavigationManager.loadCenterView("/fxml/vendor/ExtraOrdersView.fxml");
    }

    @FXML
    public void handleNavHolidays() {
        setActiveNav(navHolidaysBtn, "Customer Delivery Pause Requests");
        NavigationManager.loadCenterView("/fxml/vendor/HolidayRequestsView.fxml");
    }

    @FXML
    public void handleNavRoutePlanner() {
        setActiveNav(navRoutePlannerBtn, "Nagpur Doorstep Delivery Route Planner");
        NavigationManager.loadCenterView("/fxml/vendor/RoutePlannerView.fxml");
    }

    @FXML
    public void handleNavNotifications() {
        setActiveNav(navNotificationsBtn, "System Alerts & Notifications");
        NavigationManager.loadCenterView("/fxml/vendor/NotificationsView.fxml");
    }

    @FXML
    public void handleNavKnowledge() {
        setActiveNav(navKnowledgeBtn, "Dairy Knowledge & AI Vision Assistant");
        NavigationManager.loadCenterView("/fxml/common/KnowledgeAssistantView.fxml");
    }

    @FXML
    public void handleLogout() {
        SessionManager.clearSession();
        NavigationManager.switchScene("/fxml/auth/LoginView.fxml", "Login");
    }

    private void setActiveNav(Button btn, String title) {
        if (pageTitleLabel != null) {
            pageTitleLabel.setText(title);
        }

        if (currentActiveBtn != null) {
            currentActiveBtn.getStyleClass().remove("active");
        }
        if (btn != null) {
            if (!btn.getStyleClass().contains("active")) {
                btn.getStyleClass().add("active");
            }
            currentActiveBtn = btn;
        }
    }
}
