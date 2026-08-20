package com.agrodairy.management.exception;

/**
 * Custom runtime exception thrown when attempting to record duplicate delivery for same customer, date, and shift.
 */
public class DuplicateDeliveryException extends RuntimeException {
    public DuplicateDeliveryException(String message) {
        super(message);
    }

    public DuplicateDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
