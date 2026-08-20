package com.agrodairy.management.controller;

import com.agrodairy.management.dao.CustomerDAO;
import com.agrodairy.management.dao.impl.CustomerDAOImpl;
import com.agrodairy.management.exception.AuthenticationException;
import com.agrodairy.management.exception.ValidationException;
import com.agrodairy.management.model.Customer;
import com.agrodairy.management.model.UserLogin;
import com.agrodairy.management.model.enums.Role;
import com.agrodairy.management.service.AuthService;
import com.agrodairy.management.service.impl.AuthServiceImpl;
import com.agrodairy.management.util.NavigationManager;
import com.agrodairy.management.util.SessionManager;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class LoginController {
    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Button loginButton;

    private final AuthService authService;
    private final CustomerDAO customerDAO;

    public LoginController() {
        this.authService = new AuthServiceImpl();
        this.customerDAO = new CustomerDAOImpl();
    }

    @FXML
    public void initialize() {
        if (errorLabel != null) {
            errorLabel.setVisible(false);
        }
    }

    @FXML
    public void handleLogin() {
        String username = usernameField.getText() != null ? usernameField.getText().trim() : "";
        String password = passwordField.getText() != null ? passwordField.getText().trim() : "";

        if (username.isEmpty() || password.isEmpty()) {
            showError("Please enter both username and password.");
            return;
        }

        try {
            UserLogin user = authService.login(username, password);
            logger.info("Login successful for user: {} with role: {}", user.getUsername(), user.getRole());

            if (user.getRole() == Role.VENDOR) {
                SessionManager.setSession(user);
                NavigationManager.switchScene("/fxml/vendor/VendorMainLayout.fxml", "Vendor Dashboard");
            } else if (user.getRole() == Role.CUSTOMER) {
                Optional<Customer> custOpt = customerDAO.findByUserId(user.getUserId());
                Customer customer = custOpt.orElse(null);
                SessionManager.setSession(user, customer);
                NavigationManager.switchScene("/fxml/customer/CustomerMainLayout.fxml", "Customer Dashboard");
            } else {
                showError("Unknown role assigned to user.");
            }
        } catch (AuthenticationException | ValidationException e) {
            showError(e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error during login", e);
            showError("An unexpected error occurred. Please try again.");
        }
    }

    private void showError(String msg) {
        if (errorLabel != null) {
            errorLabel.setText(msg);
            errorLabel.setVisible(true);
        }
    }
}
