package com.agrodairy.management.util;

import com.agrodairy.management.model.Customer;
import com.agrodairy.management.model.UserLogin;
import com.agrodairy.management.model.enums.Role;

/**
 * Global Session Manager tracking active logged-in user & customer context.
 */
public class SessionManager {

    private static UserLogin currentUser;
    private static Customer currentCustomer;

    private SessionManager() {}

    public static synchronized void setSession(UserLogin user) {
        currentUser = user;
        currentCustomer = null;
    }

    public static synchronized void setSession(UserLogin user, Customer customer) {
        currentUser = user;
        currentCustomer = customer;
    }

    public static synchronized UserLogin getCurrentUser() {
        return currentUser;
    }

    public static synchronized Customer getCurrentCustomer() {
        return currentCustomer;
    }

    public static synchronized boolean isLoggedIn() {
        return currentUser != null;
    }

    public static synchronized boolean isVendor() {
        return currentUser != null && currentUser.getRole() == Role.VENDOR;
    }

    public static synchronized boolean isCustomer() {
        return currentUser != null && currentUser.getRole() == Role.CUSTOMER;
    }

    public static synchronized void clearSession() {
        currentUser = null;
        currentCustomer = null;
    }
}
