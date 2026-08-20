package com.agrodairy.management.controller;

import com.agrodairy.management.model.Customer;
import com.agrodairy.management.model.UserLogin;
import com.agrodairy.management.util.NavigationManager;
import com.agrodairy.management.util.SessionManager;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;

public class CustomerMainLayoutController {

    @FXML private BorderPane mainBorderPane;
    @FXML private Label pageTitleLabel;
    @FXML private Label userWelcomeLabel;

    @FXML
    public void initialize() {
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
        setPageTitle("Customer Overview Dashboard");
        NavigationManager.loadCenterView("/fxml/customer/CustomerDashboardView.fxml");
    }

    @FXML
    public void handleNavDeliveries() {
        setPageTitle("My Daily Milk Delivery Status");
        NavigationManager.loadCenterView("/fxml/customer/CustomerDeliveriesView.fxml");
    }

    @FXML
    public void handleNavBills() {
        setPageTitle("My Monthly Bills & Payments");
        NavigationManager.loadCenterView("/fxml/customer/CustomerBillsView.fxml");
    }

    @FXML
    public void handleNavHoliday() {
        setPageTitle("Request Delivery Pause / Holiday");
        NavigationManager.loadCenterView("/fxml/customer/CustomerHolidayView.fxml");
    }

    @FXML
    public void handleNavExtraOrders() {
        setPageTitle("Order Extra Milk & Dairy Products");
        NavigationManager.loadCenterView("/fxml/customer/CustomerExtraOrdersView.fxml");
    }

    @FXML
    public void handleNavKnowledge() {
        setPageTitle("Offline Dairy Knowledge Assistant");
        NavigationManager.loadCenterView("/fxml/common/KnowledgeAssistantView.fxml");
    }

    @FXML
    public void handleLogout() {
        SessionManager.clearSession();
        NavigationManager.switchScene("/fxml/auth/LoginView.fxml", "Login");
    }

    private void setPageTitle(String title) {
        if (pageTitleLabel != null) {
            pageTitleLabel.setText(title);
        }
    }
}
