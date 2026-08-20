package com.agrodairy.management.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Database Connection Pool Configuration using HikariCP with graceful fallback.
 */
public class DatabaseConfig {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);
    private static HikariDataSource dataSource;
    private static boolean hikariInitialized = false;

    static {
        initDataSource();
    }

    private static synchronized void initDataSource() {
        if (hikariInitialized) return;

        String url = System.getenv(AppConstants.DB_URL_ENV);
        if (url == null || url.trim().isEmpty()) {
            url = AppConstants.DEFAULT_DB_URL;
        }

        String user = System.getenv(AppConstants.DB_USER_ENV);
        if (user == null || user.trim().isEmpty()) {
            user = AppConstants.DEFAULT_DB_USER;
        }

        String pass = System.getenv(AppConstants.DB_PASS_ENV);
        if (pass == null) {
            pass = AppConstants.DEFAULT_DB_PASS;
        }

        try {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(url);
            config.setUsername(user);
            config.setPassword(pass);
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");
            config.setMaximumPoolSize(10);
            config.setMinimumIdle(2);
            config.setIdleTimeout(30000);
            config.setConnectionTimeout(10000);
            config.setLeakDetectionThreshold(15000);
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

            dataSource = new HikariDataSource(config);
            hikariInitialized = true;
            logger.info("HikariCP DataSource initialized successfully for: {}", url);
        } catch (Exception e) {
            logger.warn("HikariCP DataSource initialization deferred/failed: {}", e.getMessage());
            hikariInitialized = false;
        }
    }

    public static Connection getConnection() throws SQLException {
        if (hikariInitialized && dataSource != null && !dataSource.isClosed()) {
            try {
                return dataSource.getConnection();
            } catch (SQLException e) {
                logger.warn("HikariCP connection failed, attempting direct connection fallback: {}", e.getMessage());
            }
        }

        // Direct DriverManager fallback if pool is uninitialized or DB server offline during pool start
        String url = System.getenv(AppConstants.DB_URL_ENV) != null ? System.getenv(AppConstants.DB_URL_ENV) : AppConstants.DEFAULT_DB_URL;
        String user = System.getenv(AppConstants.DB_USER_ENV) != null ? System.getenv(AppConstants.DB_USER_ENV) : AppConstants.DEFAULT_DB_USER;
        String pass = System.getenv(AppConstants.DB_PASS_ENV) != null ? System.getenv(AppConstants.DB_PASS_ENV) : AppConstants.DEFAULT_DB_PASS;
        return DriverManager.getConnection(url, user, pass);
    }

    public static DataSource getDataSource() {
        return dataSource;
    }

    public static synchronized void closePool() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            hikariInitialized = false;
            logger.info("HikariCP DataSource closed.");
        }
    }
}
