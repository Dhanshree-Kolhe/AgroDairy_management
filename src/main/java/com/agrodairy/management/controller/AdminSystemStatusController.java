package com.agrodairy.management.controller;

import com.agrodairy.management.config.AppConstants;
import com.agrodairy.management.config.DatabaseConfig;
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
import com.agrodairy.management.util.AlertUtils;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Controller for the Admin System Status & Diagnostics Screen.
 * Performs real-time runtime, database, and repository health checks.
 */
public class AdminSystemStatusController {

    private static final Logger logger = LoggerFactory.getLogger(AdminSystemStatusController.class);

    @FXML private Label dbStatusBadge;
    @FXML private Label dbUrlLabel;
    @FXML private Label dbLatencyLabel;
    @FXML private Label appStatusBadge;
    @FXML private Label appUptimeLabel;
    @FXML private Label jvmVersionLabel;
    @FXML private Label memoryUsageLabel;
    @FXML private ProgressBar memoryProgressBar;

    @FXML private Label usersDataStatusLabel;
    @FXML private Label vendorsDataStatusLabel;
    @FXML private Label customersDataStatusLabel;
    @FXML private Label productsDataStatusLabel;
    @FXML private Label paymentsDataStatusLabel;

    @FXML private Label lastCheckedLabel;

    private static final long APP_START_TIME = System.currentTimeMillis();

    private final UserLoginDAO userLoginDAO;
    private final VendorDAO vendorDAO;
    private final CustomerDAO customerDAO;
    private final ProductDAO productDAO;
    private final PaymentDAO paymentDAO;

    public AdminSystemStatusController() {
        this.userLoginDAO = new UserLoginDAOImpl();
        this.vendorDAO = new VendorDAOImpl();
        this.customerDAO = new CustomerDAOImpl();
        this.productDAO = new ProductDAOImpl();
        this.paymentDAO = new PaymentDAOImpl();
    }

    @FXML
    public void initialize() {
        runDiagnostics();
    }

    @FXML
    public void runDiagnostics() {
        logger.info("Executing comprehensive live system diagnostics check...");
        checkDatabaseConnectivity();
        checkApplicationRuntime();
        checkDataRepositories();

        if (lastCheckedLabel != null) {
            String timeStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss"));
            lastCheckedLabel.setText("Last Diagnostics Check: " + timeStr);
        }
    }

    private void checkDatabaseConnectivity() {
        long startTime = System.currentTimeMillis();
        boolean connected = false;
        long latency = 0;
        String errorMessage = null;

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeQuery("SELECT 1");
            latency = System.currentTimeMillis() - startTime;
            connected = true;
        } catch (Exception e) {
            errorMessage = e.getMessage();
            logger.warn("Database connectivity diagnostics failed: {}", errorMessage);
        }

        if (connected) {
            if (dbStatusBadge != null) {
                dbStatusBadge.setText("CONNECTED (Healthy)");
                dbStatusBadge.setStyle("-fx-text-fill: #059669; -fx-font-weight: bold; -fx-background-color: #ECFDF5; -fx-padding: 4 10; -fx-background-radius: 4;");
            }
            if (dbLatencyLabel != null) {
                dbLatencyLabel.setText(latency + " ms");
            }
        } else {
            if (dbStatusBadge != null) {
                dbStatusBadge.setText("DISCONNECTED (Offline)");
                dbStatusBadge.setStyle("-fx-text-fill: #DC2626; -fx-font-weight: bold; -fx-background-color: #FEF2F2; -fx-padding: 4 10; -fx-background-radius: 4;");
            }
            if (dbLatencyLabel != null) {
                dbLatencyLabel.setText("N/A (" + (errorMessage != null ? errorMessage : "Timeout") + ")");
            }
        }

        if (dbUrlLabel != null) {
            dbUrlLabel.setText(AppConstants.DEFAULT_DB_URL);
        }
    }

    private void checkApplicationRuntime() {
        if (appStatusBadge != null) {
            appStatusBadge.setText("OPERATIONAL");
            appStatusBadge.setStyle("-fx-text-fill: #059669; -fx-font-weight: bold; -fx-background-color: #ECFDF5; -fx-padding: 4 10; -fx-background-radius: 4;");
        }

        // JVM & OS Information
        if (jvmVersionLabel != null) {
            String javaVer = System.getProperty("java.version");
            String javaVendor = System.getProperty("java.vendor");
            String osName = System.getProperty("os.name");
            jvmVersionLabel.setText("Java " + javaVer + " (" + javaVendor + ") on " + osName);
        }

        // Memory Usage
        Runtime runtime = Runtime.getRuntime();
        long totalMem = runtime.totalMemory() / (1024 * 1024);
        long freeMem = runtime.freeMemory() / (1024 * 1024);
        long usedMem = totalMem - freeMem;
        long maxMem = runtime.maxMemory() / (1024 * 1024);

        if (memoryUsageLabel != null) {
            memoryUsageLabel.setText(String.format("%d MB Used / %d MB Allocated (Max: %d MB)", usedMem, totalMem, maxMem));
        }

        if (memoryProgressBar != null) {
            double ratio = (double) usedMem / (double) totalMem;
            memoryProgressBar.setProgress(Math.min(1.0, Math.max(0.0, ratio)));
        }

        // Uptime
        if (appUptimeLabel != null) {
            long uptimeSeconds = (System.currentTimeMillis() - APP_START_TIME) / 1000;
            long mins = uptimeSeconds / 60;
            long secs = uptimeSeconds % 60;
            appUptimeLabel.setText(String.format("%d min %d sec", mins, secs));
        }
    }

    private void checkDataRepositories() {
        // Users repository
        try {
            int users = userLoginDAO.findAll().size();
            if (usersDataStatusLabel != null) {
                usersDataStatusLabel.setText("Available (" + users + " records verified)");
                usersDataStatusLabel.setStyle("-fx-text-fill: #059669;");
            }
        } catch (Exception e) {
            if (usersDataStatusLabel != null) {
                usersDataStatusLabel.setText("Unavailable (" + e.getMessage() + ")");
                usersDataStatusLabel.setStyle("-fx-text-fill: #DC2626;");
            }
        }

        // Vendors repository
        try {
            int vendors = vendorDAO.findAll().size();
            if (vendorsDataStatusLabel != null) {
                vendorsDataStatusLabel.setText("Available (" + vendors + " records verified)");
                vendorsDataStatusLabel.setStyle("-fx-text-fill: #059669;");
            }
        } catch (Exception e) {
            if (vendorsDataStatusLabel != null) {
                vendorsDataStatusLabel.setText("Unavailable (" + e.getMessage() + ")");
                vendorsDataStatusLabel.setStyle("-fx-text-fill: #DC2626;");
            }
        }

        // Customers repository
        try {
            int customers = customerDAO.findAll().size();
            if (customersDataStatusLabel != null) {
                customersDataStatusLabel.setText("Available (" + customers + " records verified)");
                customersDataStatusLabel.setStyle("-fx-text-fill: #059669;");
            }
        } catch (Exception e) {
            if (customersDataStatusLabel != null) {
                customersDataStatusLabel.setText("Unavailable (" + e.getMessage() + ")");
                customersDataStatusLabel.setStyle("-fx-text-fill: #DC2626;");
            }
        }

        // Products repository
        try {
            int products = productDAO.findAll().size();
            if (productsDataStatusLabel != null) {
                productsDataStatusLabel.setText("Available (" + products + " records verified)");
                productsDataStatusLabel.setStyle("-fx-text-fill: #059669;");
            }
        } catch (Exception e) {
            if (productsDataStatusLabel != null) {
                productsDataStatusLabel.setText("Unavailable (" + e.getMessage() + ")");
                productsDataStatusLabel.setStyle("-fx-text-fill: #DC2626;");
            }
        }

        // Payments repository
        try {
            int payments = paymentDAO.findAll().size();
            if (paymentsDataStatusLabel != null) {
                paymentsDataStatusLabel.setText("Available (" + payments + " records verified)");
                paymentsDataStatusLabel.setStyle("-fx-text-fill: #059669;");
            }
        } catch (Exception e) {
            if (paymentsDataStatusLabel != null) {
                paymentsDataStatusLabel.setText("Unavailable (" + e.getMessage() + ")");
                paymentsDataStatusLabel.setStyle("-fx-text-fill: #DC2626;");
            }
        }
    }
}
