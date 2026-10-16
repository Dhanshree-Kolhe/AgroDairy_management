package com.agrodairy.management.controller;

import com.agrodairy.management.dao.UserLoginDAO;
import com.agrodairy.management.dao.impl.UserLoginDAOImpl;
import com.agrodairy.management.model.UserLogin;
import com.agrodairy.management.model.enums.Role;
import com.agrodairy.management.util.AlertUtils;
import com.agrodairy.management.util.SessionManager;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * Controller for Admin User Management.
 * Provides search, role/status filters, and secure Activate/Deactivate operations.
 */
public class AdminUsersController {

    private static final Logger logger = LoggerFactory.getLogger(AdminUsersController.class);

    @FXML private TextField searchField;
    @FXML private ComboBox<String> roleFilterCombo;
    @FXML private ComboBox<String> statusFilterCombo;
    @FXML private Label userCountBadge;

    @FXML private TableView<UserLogin> usersTable;
    @FXML private TableColumn<UserLogin, String> colUserId;
    @FXML private TableColumn<UserLogin, String> colUsername;
    @FXML private TableColumn<UserLogin, String> colFullName;
    @FXML private TableColumn<UserLogin, String> colRole;
    @FXML private TableColumn<UserLogin, String> colEmail;
    @FXML private TableColumn<UserLogin, String> colPhone;
    @FXML private TableColumn<UserLogin, String> colStatus;

    private final UserLoginDAO userLoginDAO;
    private FilteredList<UserLogin> filteredUsers;

    public AdminUsersController() {
        this.userLoginDAO = new UserLoginDAOImpl();
    }

    public AdminUsersController(UserLoginDAO userLoginDAO) {
        this.userLoginDAO = userLoginDAO;
    }

    @FXML
    public void initialize() {
        setupFilters();
        setupTableColumns();
        loadUsers();
    }

    private void setupFilters() {
        if (roleFilterCombo != null) {
            roleFilterCombo.setItems(FXCollections.observableArrayList("ALL", "ADMIN", "VENDOR", "CUSTOMER"));
            roleFilterCombo.setValue("ALL");
            roleFilterCombo.valueProperty().addListener((obs, oldV, newV) -> applyFilter());
        }

        if (statusFilterCombo != null) {
            statusFilterCombo.setItems(FXCollections.observableArrayList("ALL", "ACTIVE", "INACTIVE"));
            statusFilterCombo.setValue("ALL");
            statusFilterCombo.valueProperty().addListener((obs, oldV, newV) -> applyFilter());
        }

        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldV, newV) -> applyFilter());
        }
    }

    private void setupTableColumns() {
        if (colUserId != null) {
            colUserId.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getUserId())));
        }
        if (colUsername != null) {
            colUsername.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getUsername()));
        }
        if (colFullName != null) {
            colFullName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFullName()));
        }
        if (colRole != null) {
            colRole.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getRole() != null ? d.getValue().getRole().name() : ""));
            colRole.setCellFactory(column -> new TableCell<>() {
                @Override
                protected void updateItem(String roleName, boolean empty) {
                    super.updateItem(roleName, empty);
                    if (empty || roleName == null) {
                        setText(null);
                        setStyle("");
                    } else {
                        setText(roleName);
                        if ("ADMIN".equals(roleName)) {
                            setStyle("-fx-text-fill: #7C3AED; -fx-font-weight: bold;");
                        } else if ("VENDOR".equals(roleName)) {
                            setStyle("-fx-text-fill: #2563EB; -fx-font-weight: bold;");
                        } else {
                            setStyle("-fx-text-fill: #059669; -fx-font-weight: bold;");
                        }
                    }
                }
            });
        }
        if (colEmail != null) {
            colEmail.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEmail() != null ? d.getValue().getEmail() : "—"));
        }
        if (colPhone != null) {
            colPhone.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getPhone() != null ? d.getValue().getPhone() : "—"));
        }
        if (colStatus != null) {
            colStatus.setCellValueFactory(d -> new SimpleStringProperty(Boolean.TRUE.equals(d.getValue().getActive()) ? "ACTIVE" : "INACTIVE"));
            colStatus.setCellFactory(column -> new TableCell<>() {
                @Override
                protected void updateItem(String status, boolean empty) {
                    super.updateItem(status, empty);
                    if (empty || status == null) {
                        setText(null);
                        setStyle("");
                    } else {
                        setText(status);
                        if ("ACTIVE".equals(status)) {
                            setStyle("-fx-text-fill: #059669; -fx-font-weight: bold;");
                        } else {
                            setStyle("-fx-text-fill: #DC2626; -fx-font-weight: bold;");
                        }
                    }
                }
            });
        }
    }

    @FXML
    public void loadUsers() {
        try {
            List<UserLogin> users = userLoginDAO.findAll();
            filteredUsers = new FilteredList<>(FXCollections.observableArrayList(users), p -> true);
            if (usersTable != null) {
                usersTable.setItems(filteredUsers);
            }
            applyFilter();
        } catch (Exception e) {
            logger.error("Failed to load user list", e);
            AlertUtils.showError("Database Error", "Failed to retrieve user accounts: " + e.getMessage());
        }
    }

    private void applyFilter() {
        if (filteredUsers == null) return;

        String query = searchField != null && searchField.getText() != null ? searchField.getText().trim().toLowerCase() : "";
        String roleFilter = roleFilterCombo != null ? roleFilterCombo.getValue() : "ALL";
        String statusFilter = statusFilterCombo != null ? statusFilterCombo.getValue() : "ALL";

        filteredUsers.setPredicate(user -> {
            // Text Search
            if (!query.isEmpty()) {
                boolean matchUsername = user.getUsername() != null && user.getUsername().toLowerCase().contains(query);
                boolean matchName = user.getFullName() != null && user.getFullName().toLowerCase().contains(query);
                boolean matchEmail = user.getEmail() != null && user.getEmail().toLowerCase().contains(query);
                boolean matchPhone = user.getPhone() != null && user.getPhone().toLowerCase().contains(query);
                if (!matchUsername && !matchName && !matchEmail && !matchPhone) {
                    return false;
                }
            }

            // Role Filter
            if (roleFilter != null && !roleFilter.equals("ALL")) {
                if (user.getRole() == null || !user.getRole().name().equalsIgnoreCase(roleFilter)) {
                    return false;
                }
            }

            // Status Filter
            if (statusFilter != null && !statusFilter.equals("ALL")) {
                boolean isActive = Boolean.TRUE.equals(user.getActive());
                if (statusFilter.equals("ACTIVE") && !isActive) return false;
                if (statusFilter.equals("INACTIVE") && isActive) return false;
            }

            return true;
        });

        if (userCountBadge != null) {
            userCountBadge.setText("Showing: " + filteredUsers.size() + " accounts");
        }
    }

    @FXML
    public void handleActivateUser() {
        UserLogin selected = getSelectedUser();
        if (selected == null) return;

        if (Boolean.TRUE.equals(selected.getActive())) {
            AlertUtils.showInfo("Already Active", "User '" + selected.getUsername() + "' is already in ACTIVE status.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Account Activation");
        confirm.setHeaderText("Activate Account: " + selected.getUsername());
        confirm.setContentText("Are you sure you want to activate the account for " + selected.getFullName() + "? They will be able to log in again.");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                boolean success = userLoginDAO.updateStatus(selected.getUserId(), true);
                if (success) {
                    AlertUtils.showInfo("Success", "Account '" + selected.getUsername() + "' has been successfully ACTIVATED.");
                    loadUsers();
                } else {
                    AlertUtils.showError("Failure", "Could not update status for user: " + selected.getUsername());
                }
            } catch (Exception e) {
                logger.error("Failed to activate user: {}", selected.getUsername(), e);
                AlertUtils.showError("Database Error", "Failed to activate user: " + e.getMessage());
            }
        }
    }

    @FXML
    public void handleDeactivateUser() {
        UserLogin selected = getSelectedUser();
        if (selected == null) return;

        if (!Boolean.TRUE.equals(selected.getActive())) {
            AlertUtils.showInfo("Already Inactive", "User '" + selected.getUsername() + "' is already INACTIVE.");
            return;
        }

        // Security Requirement 1: Cannot deactivate own current logged-in account
        UserLogin currentUser = SessionManager.getCurrentUser();
        if (currentUser != null && (selected.getUserId().equals(currentUser.getUserId()) || selected.getUsername().equalsIgnoreCase(currentUser.getUsername()))) {
            AlertUtils.showError("Security Violation", "You cannot deactivate your own currently logged-in Administrator account.");
            return;
        }

        // Security Requirement 2: Protect the last usable Admin account from accidental deactivation
        if (selected.getRole() == Role.ADMIN) {
            List<UserLogin> allUsers = userLoginDAO.findAll();
            long activeAdmins = allUsers.stream()
                    .filter(u -> u.getRole() == Role.ADMIN && Boolean.TRUE.equals(u.getActive()))
                    .count();
            if (activeAdmins <= 1) {
                AlertUtils.showError("Security Protection", "Cannot deactivate the last active Administrator account in the system. At least one active admin account must remain.");
                return;
            }
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Account Deactivation");
        confirm.setHeaderText("Deactivate Account: " + selected.getUsername());
        confirm.setContentText("Are you sure you want to deactivate the account for " + selected.getFullName() + "?\n\nThe user record will remain in the database, but they will be blocked from logging in.");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                boolean success = userLoginDAO.updateStatus(selected.getUserId(), false);
                if (success) {
                    AlertUtils.showInfo("Success", "Account '" + selected.getUsername() + "' has been DEACTIVATED.");
                    loadUsers();
                } else {
                    AlertUtils.showError("Failure", "Could not update status for user: " + selected.getUsername());
                }
            } catch (Exception e) {
                logger.error("Failed to deactivate user: {}", selected.getUsername(), e);
                AlertUtils.showError("Database Error", "Failed to deactivate user: " + e.getMessage());
            }
        }
    }

    @FXML
    public void handleResetFilters() {
        if (searchField != null) searchField.clear();
        if (roleFilterCombo != null) roleFilterCombo.setValue("ALL");
        if (statusFilterCombo != null) statusFilterCombo.setValue("ALL");
        loadUsers();
    }

    private UserLogin getSelectedUser() {
        if (usersTable == null) return null;
        UserLogin selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtils.showWarning("Selection Required", "Please select a user account from the table first.");
            return null;
        }
        return selected;
    }
}
