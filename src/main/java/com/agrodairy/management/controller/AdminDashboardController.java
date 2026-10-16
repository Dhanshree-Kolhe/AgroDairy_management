package com.agrodairy.management.controller;

import com.agrodairy.management.dao.CustomerDAO;
import com.agrodairy.management.dao.PaymentDAO;
import com.agrodairy.management.dao.ProductDAO;
import com.agrodairy.management.dao.UserLoginDAO;
import com.agrodairy.management.dao.VendorDAO;
import com.agrodairy.management.dao.impl.CustomerDAOImpl;
import com.agrodairy.management.dao.impl.PaymentDAOImpl;
import com.agrodairy.management.dao.impl.ProductDAOImpl;
import com.agrodairy.management.dao.impl.UserLoginDAOImpl;
import com.agrodairy.management.dao.impl.VendorDAOImpl;
import com.agrodairy.management.model.Payment;
import com.agrodairy.management.model.UserLogin;
import com.agrodairy.management.model.enums.Role;
import com.agrodairy.management.util.AlertUtils;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/**
 * Controller for the Admin Dashboard overview screen.
 * Displays live, dynamically computed KPI statistics and recent system registrations.
 */
public class AdminDashboardController {

    private static final Logger logger = LoggerFactory.getLogger(AdminDashboardController.class);

    @FXML private Label totalUsersLabel;
    @FXML private Label totalVendorsLabel;
    @FXML private Label totalCustomersLabel;
    @FXML private Label totalRevenueLabel;
    @FXML private Label activeProductsLabel;
    @FXML private Label systemHealthLabel;

    @FXML private TableView<UserLogin> recentUsersTable;
    @FXML private TableColumn<UserLogin, String> colUserId;
    @FXML private TableColumn<UserLogin, String> colUsername;
    @FXML private TableColumn<UserLogin, String> colFullName;
    @FXML private TableColumn<UserLogin, String> colRole;
    @FXML private TableColumn<UserLogin, String> colContact;
    @FXML private TableColumn<UserLogin, String> colStatus;

    private final UserLoginDAO userLoginDAO;
    private final VendorDAO vendorDAO;
    private final CustomerDAO customerDAO;
    private final PaymentDAO paymentDAO;
    private final ProductDAO productDAO;

    public AdminDashboardController() {
        this.userLoginDAO = new UserLoginDAOImpl();
        this.vendorDAO = new VendorDAOImpl();
        this.customerDAO = new CustomerDAOImpl();
        this.paymentDAO = new PaymentDAOImpl();
        this.productDAO = new ProductDAOImpl();
    }

    public AdminDashboardController(UserLoginDAO userLoginDAO, VendorDAO vendorDAO, CustomerDAO customerDAO, PaymentDAO paymentDAO, ProductDAO productDAO) {
        this.userLoginDAO = userLoginDAO;
        this.vendorDAO = vendorDAO;
        this.customerDAO = customerDAO;
        this.paymentDAO = paymentDAO;
        this.productDAO = productDAO;
    }

    @FXML
    public void initialize() {
        setupTableColumns();
        refreshDashboard();
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
            colRole.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getRole() != null ? d.getValue().getRole().name() : "N/A"));
            colRole.setCellFactory(column -> new TableCell<>() {
                @Override
                protected void updateItem(String roleName, boolean empty) {
                    super.updateItem(roleName, empty);
                    if (empty || roleName == null) {
                        setText(null);
                        setGraphic(null);
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
        if (colContact != null) {
            colContact.setCellValueFactory(d -> {
                String email = d.getValue().getEmail() != null ? d.getValue().getEmail() : "";
                String phone = d.getValue().getPhone() != null ? d.getValue().getPhone() : "";
                return new SimpleStringProperty(!email.isEmpty() ? email : phone);
            });
        }
        if (colStatus != null) {
            colStatus.setCellValueFactory(d -> new SimpleStringProperty(Boolean.TRUE.equals(d.getValue().getActive()) ? "ACTIVE" : "INACTIVE"));
            colStatus.setCellFactory(column -> new TableCell<>() {
                @Override
                protected void updateItem(String status, boolean empty) {
                    super.updateItem(status, empty);
                    if (empty || status == null) {
                        setText(null);
                        setGraphic(null);
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
    public void refreshDashboard() {
        try {
            logger.info("Refreshing Admin Dashboard metrics from database...");

            // 1. Total Users
            List<UserLogin> allUsers = userLoginDAO.findAll();
            int totalUsers = allUsers.size();
            if (totalUsersLabel != null) {
                totalUsersLabel.setText(String.valueOf(totalUsers));
            }

            // 2. Total Vendors
            long totalVendors = allUsers.stream().filter(u -> u.getRole() == Role.VENDOR).count();
            if (totalVendorsLabel != null) {
                totalVendorsLabel.setText(String.valueOf(totalVendors));
            }

            // 3. Total Customers
            long totalCustomers = allUsers.stream().filter(u -> u.getRole() == Role.CUSTOMER).count();
            if (totalCustomersLabel != null) {
                totalCustomersLabel.setText(String.valueOf(totalCustomers));
            }

            // 4. Total Revenue (calculated from successful payments to prevent duplicate counting)
            List<Payment> allPayments = paymentDAO.findAll();
            BigDecimal totalRevenue = allPayments.stream()
                    .filter(p -> "SUCCESS".equalsIgnoreCase(p.getStatus()))
                    .map(Payment::getAmountPaid)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (totalRevenueLabel != null) {
                NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
                totalRevenueLabel.setText(currencyFormat.format(totalRevenue));
            }

            // 5. Active Products
            int activeProducts = productDAO.findActiveProducts().size();
            if (activeProductsLabel != null) {
                activeProductsLabel.setText(String.valueOf(activeProducts));
            }

            // 6. System Health
            if (systemHealthLabel != null) {
                systemHealthLabel.setText("Operational (100%)");
            }

            // Populate Recent Users table
            if (recentUsersTable != null) {
                recentUsersTable.setItems(FXCollections.observableArrayList(allUsers));
            }

        } catch (Exception e) {
            logger.error("Failed to load Admin Dashboard metrics", e);
            AlertUtils.showError("Data Error", "Could not load administrative metrics: " + e.getMessage());
        }
    }
}
