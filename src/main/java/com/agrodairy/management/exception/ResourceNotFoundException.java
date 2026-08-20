package com.agrodairy.management.exception;

/**
 * Custom runtime exception thrown when requested entity is not found in database.
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
