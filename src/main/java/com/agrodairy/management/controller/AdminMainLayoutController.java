package com.agrodairy.management.controller;

import com.agrodairy.management.model.UserLogin;
import com.agrodairy.management.util.AlertUtils;
import com.agrodairy.management.util.NavigationManager;
import com.agrodairy.management.util.SessionManager;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller for the Admin Main Application Shell & Sidebar Navigation.
 */
public class AdminMainLayoutController {

    private static final Logger logger = LoggerFactory.getLogger(AdminMainLayoutController.class);

    @FXML private BorderPane mainBorderPane;
    @FXML private Label pageTitleLabel;
    @FXML private Label userWelcomeLabel;

    @FXML private Button navDashboardBtn;
    @FXML private Button navUsersBtn;
    @FXML private Button navProductsBtn;
    @FXML private Button navSystemStatusBtn;

    private Button currentActiveBtn;

    @FXML
    public void initialize() {
        if (!SessionManager.isAdmin()) {
            logger.error("Security alert: Unauthorized access attempt to Admin console.");
            AlertUtils.showError("Access Denied", "Administrative privileges are required to access this console.");
            handleLogout();
            return;
        }

        NavigationManager.setMainLayoutPane(mainBorderPane);
        UserLogin user = SessionManager.getCurrentUser();
        if (user != null && userWelcomeLabel != null) {
            userWelcomeLabel.setText("Admin: " + user.getFullName());
        }
        handleNavDashboard();
    }

    @FXML
    public void handleNavDashboard() {
        setActiveNav(navDashboardBtn, "System Executive Overview");
        NavigationManager.loadCenterView("/fxml/admin/AdminDashboardView.fxml");
    }

    @FXML
    public void handleNavUsers() {
        setActiveNav(navUsersBtn, "User Directory & Access Control");
        NavigationManager.loadCenterView("/fxml/admin/AdminUsersView.fxml");
    }

    @FXML
    public void handleNavProducts() {
        setActiveNav(navProductsBtn, "Master Dairy Products Catalog");
        NavigationManager.loadCenterView("/fxml/admin/AdminProductsView.fxml");
    }

    @FXML
    public void handleNavSystemStatus() {
        setActiveNav(navSystemStatusBtn, "System Health & Database Diagnostics");
        NavigationManager.loadCenterView("/fxml/admin/AdminSystemStatusView.fxml");
    }

    @FXML
    public void handleLogout() {
        logger.info("Admin user logging out: {}", SessionManager.getCurrentUser() != null ? SessionManager.getCurrentUser().getUsername() : "Unknown");
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
