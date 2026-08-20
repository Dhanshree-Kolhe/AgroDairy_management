package com.agrodairy.management.util;

import org.mindrot.jbcrypt.BCrypt;

/**
 * BCrypt password hashing and verification utility.
 */
public final class PasswordUtil {
    private static final int COST_FACTOR = 12;

    private PasswordUtil() {}

    public static String hashPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty for hashing.");
        }
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(COST_FACTOR));
    }

    public static boolean checkPassword(String plainPassword, String hashedPassword) {
        if (plainPassword == null || hashedPassword == null) {
            return false;
        }
        try {
            return BCrypt.checkpw(plainPassword, hashedPassword);
        } catch (Exception e) {
            return false;
        }
    }
}
