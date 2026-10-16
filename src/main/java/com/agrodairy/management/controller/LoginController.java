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
    @FXML private TextField passwordTextField;
    @FXML private Button togglePasswordBtn;
    @FXML private Label errorLabel;
    @FXML private Button loginButton;

    private boolean isPasswordVisible = false;

    private final AuthService authService;
    private final CustomerDAO customerDAO;

    public LoginController() {
        this.authService = new AuthServiceImpl();
        this.customerDAO = new CustomerDAOImpl();
    }

    public LoginController(AuthService authService, CustomerDAO customerDAO) {
        this.authService = authService;
        this.customerDAO = customerDAO;
    }

    @FXML
    public void initialize() {
        if (errorLabel != null) {
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
        }
        if (passwordTextField != null) {
            passwordTextField.setVisible(false);
            passwordTextField.setManaged(false);
        }
    }

    @FXML
    public void handleTogglePassword() {
        isPasswordVisible = !isPasswordVisible;

        if (isPasswordVisible) {
            if (passwordTextField != null && passwordField != null) {
                passwordTextField.setText(passwordField.getText());
                passwordTextField.setVisible(true);
                passwordTextField.setManaged(true);
                passwordField.setVisible(false);
                passwordField.setManaged(false);
                passwordTextField.requestFocus();
                passwordTextField.positionCaret(passwordTextField.getText().length());
            }
            if (togglePasswordBtn != null) {
                togglePasswordBtn.setText("🙈 Hide");
            }
        } else {
            if (passwordTextField != null && passwordField != null) {
                passwordField.setText(passwordTextField.getText());
                passwordField.setVisible(true);
                passwordField.setManaged(true);
                passwordTextField.setVisible(false);
                passwordTextField.setManaged(false);
                passwordField.requestFocus();
                passwordField.positionCaret(passwordField.getText().length());
            }
            if (togglePasswordBtn != null) {
                togglePasswordBtn.setText("👁️ Show");
            }
        }
    }

    @FXML
    public void handleFillAdminDemo() {
        setCredentials("admin_super", "secret123");
    }

    @FXML
    public void handleFillVendorDemo() {
        setCredentials("vendor_ramesh", "secret123");
    }

    @FXML
    public void handleFillCustomerDemo() {
        setCredentials("cust_rahul", "secret123");
    }

    private void setCredentials(String username, String password) {
        if (usernameField != null) usernameField.setText(username);
        if (passwordField != null) passwordField.setText(password);
        if (passwordTextField != null) passwordTextField.setText(password);
        hideError();
        if (loginButton != null) loginButton.requestFocus();
    }

    @FXML
    public void handleLogin() {
        String username = usernameField != null && usernameField.getText() != null ? usernameField.getText().trim() : "";
        String password = getActivePassword();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Please enter both username and password.");
            return;
        }

        try {
            UserLogin user = authService.login(username, password);
            logger.info("Login successful for user: {} with role: {}", user.getUsername(), user.getRole());

            if (user.getRole() == Role.ADMIN) {
                SessionManager.setSession(user);
                NavigationManager.switchScene("/fxml/admin/AdminMainLayout.fxml", "Admin Console");
            } else if (user.getRole() == Role.VENDOR) {
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

    private String getActivePassword() {
        if (isPasswordVisible && passwordTextField != null && passwordTextField.getText() != null) {
            return passwordTextField.getText().trim();
        }
        if (passwordField != null && passwordField.getText() != null) {
            return passwordField.getText().trim();
        }
        return "";
    }

    private void showError(String msg) {
        if (errorLabel != null) {
            errorLabel.setText(msg);
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
        }
    }

    private void hideError() {
        if (errorLabel != null) {
            errorLabel.setText("");
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
        }
    }
}
