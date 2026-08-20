package com.agrodairy.management.util;

import com.agrodairy.management.config.DatabaseConfig;
import com.agrodairy.management.exception.DatabaseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Utility helper class for Database connections and resource cleanup.
 */
public final class DatabaseUtil {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseUtil.class);

    private DatabaseUtil() {}

    public static Connection getConnection() {
        try {
            return DatabaseConfig.getConnection();
        } catch (SQLException e) {
            logger.error("Failed to acquire JDBC Connection: {}", e.getMessage(), e);
            throw new DatabaseException("Database connection failure: " + e.getMessage(), e);
        }
    }

    public static void closeQuietly(AutoCloseable... resources) {
        if (resources == null) return;
        for (AutoCloseable resource : resources) {
            if (resource != null) {
                try {
                    resource.close();
                } catch (Exception e) {
                    logger.debug("Quietly swallowed exception during resource close: {}", e.getMessage());
                }
            }
        }
    }

    public static void rollbackQuietly(Connection conn) {
        if (conn != null) {
            try {
                conn.rollback();
            } catch (SQLException e) {
                logger.warn("Failed to rollback connection transaction: {}", e.getMessage());
            }
        }
    }
}
