package com.agrodairy.management.config;

/**
 * Global system constants and default configuration values.
 */
public final class AppConstants {
    private AppConstants() {
    }

    public static final String APP_NAME = "Smart Dairy Management System";
    public static final String APP_VERSION = "1.0.0";

    // Database Defaults
    public static final String DB_URL_ENV = "AGRO_DAIRY_DB_URL";
    public static final String DB_USER_ENV = "AGRO_DAIRY_DB_USER";
    public static final String DB_PASS_ENV = "AGRO_DAIRY_DB_PASS";

    public static final String DEFAULT_DB_URL = "jdbc:mysql://localhost:3306/agro_dairy?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    public static final String DEFAULT_DB_USER = "root";
    public static final String DEFAULT_DB_PASS = "dksql20@";
    // Business Logic Cut-offs
    public static final int HOLIDAY_REQUEST_CUTOFF_HOUR = 22; // 10 PM cut-off for next morning delivery pause

    // Nagpur Delivery Zones
    public static final String[] NAGPUR_DELIVERY_ZONES = new String[] {
            "Dharampeth", "Pratap Nagar", "Manish Nagar", "Sadar", "Trimurti Nagar",
            "Nandanvan", "Wardha Road", "Hingna Road", "Bajaj Nagar", "Ramdaspeth"
    };
}
