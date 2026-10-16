package com.agrodairy.management.model.enums;

/**
 * Tracks the actual connection and configuration status of the AI Assistant service.
 * Prevents false claims of connection when a key is merely stored or unverified.
 */
public enum AiConnectionStatus {
    UNCONFIGURED("Unconfigured (Offline Fallback Mode)"),
    CONFIGURED("Key Configured (Untested)"),
    CONNECTED("Connected & Verified"),
    ERROR("Service / Network Error");

    private final String description;

    AiConnectionStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
