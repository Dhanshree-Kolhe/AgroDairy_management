package com.agrodairy.management.controller;

import com.agrodairy.management.model.Customer;
import com.agrodairy.management.model.UserLogin;
import com.agrodairy.management.util.NavigationManager;
import com.agrodairy.management.util.SessionManager;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;

public class CustomerMainLayoutController {

    @FXML private BorderPane mainBorderPane;
    @FXML private Label pageTitleLabel;
    @FXML private Label userWelcomeLabel;

    @FXML private Button navDashboardBtn;
    @FXML private Button navDeliveriesBtn;
    @FXML private Button navBillsBtn;
    @FXML private Button navHolidayBtn;
    @FXML private Button navExtraOrdersBtn;
    @FXML private Button navKnowledgeBtn;

    private Button currentActiveBtn;

    @FXML
    public void initialize() {
        if (!SessionManager.isCustomer()) {
            com.agrodairy.management.util.AlertUtils.showError("Access Denied", "Customer credentials are required to access this portal.");
            handleLogout();
            return;
        }

        NavigationManager.setMainLayoutPane(mainBorderPane);
        UserLogin user = SessionManager.getCurrentUser();
        Customer customer = SessionManager.getCurrentCustomer();

        if (userWelcomeLabel != null) {
            String name = (customer != null) ? customer.getFullName() : (user != null ? user.getFullName() : "Customer");
            userWelcomeLabel.setText("Welcome, " + name);
        }

        handleNavDashboard();
    }

    @FXML
    public void handleNavDashboard() {
        setActiveNav(navDashboardBtn, "Customer Overview Dashboard");
        NavigationManager.loadCenterView("/fxml/customer/CustomerDashboardView.fxml");
    }

    @FXML
    public void handleNavDeliveries() {
        setActiveNav(navDeliveriesBtn, "My Daily Milk Delivery Status");
        NavigationManager.loadCenterView("/fxml/customer/CustomerDeliveriesView.fxml");
    }

    @FXML
    public void handleNavBills() {
        setActiveNav(navBillsBtn, "My Monthly Bills & Payments");
        NavigationManager.loadCenterView("/fxml/customer/CustomerBillsView.fxml");
    }

    @FXML
    public void handleNavHoliday() {
        setActiveNav(navHolidayBtn, "Request Delivery Pause / Holiday");
        NavigationManager.loadCenterView("/fxml/customer/CustomerHolidayView.fxml");
    }

    @FXML
    public void handleNavExtraOrders() {
        setActiveNav(navExtraOrdersBtn, "Order Extra Milk & Dairy Products");
        NavigationManager.loadCenterView("/fxml/customer/CustomerExtraOrdersView.fxml");
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
