package com.agrodairy.management.service;

import com.agrodairy.management.model.enums.AiConnectionStatus;
import com.agrodairy.management.model.enums.ImageAnalysisCategory;

import java.util.concurrent.CompletableFuture;

/**
 * Service interface for live AI question answering and genuine vision-based image inspection.
 */
public interface AiAssistantService {

    /**
     * Checks if a valid AI API key (Google Gemini or OpenAI) is configured.
     */
    boolean isAiConfigured();

    /**
     * Returns the current connection and verification status.
     * Prevents misleading claims that AI is connected when it is merely configured or unverified.
     */
    AiConnectionStatus getConnectionStatus();

    /**
     * Returns human-readable diagnostic details about the current status (e.g. error message, last verified time).
     */
    String getStatusDetails();

    /**
     * Returns the name of the active AI model/provider or indicates that AI is unconfigured.
     */
    String getActiveProviderName();

    /**
     * Returns the active Gemini model name (e.g. gemini-2.5-flash).
     */
    String getGeminiModel();

    /**
     * Sets or updates the active Gemini model name at runtime.
     *
     * @param model Supported Gemini model (e.g. "gemini-2.5-flash").
     */
    void setGeminiModel(String model);

    /**
     * Returns clear instructions on how the administrator or user can configure the AI API key.
     */
    String getConfigurationInstructions();

    /**
     * Tests connectivity to the active AI provider endpoint and verifies the API key.
     *
     * @return CompletableFuture completing with true if connection was successfully verified.
     */
    CompletableFuture<Boolean> testConnectionAsync();

    /**
     * Sends a text prompt asynchronously to the AI model.
     *
     * @param prompt User question or inquiry.
     * @return CompletableFuture completing with the AI response text.
     */
    CompletableFuture<String> askAiAsync(String prompt);

    /**
     * Sends an image asynchronously to a genuine vision-capable AI model for structured analysis.
     *
     * @param imageBytes Raw image file bytes (JPG, JPEG, PNG).
     * @param mimeType   MIME type (e.g. "image/jpeg" or "image/png").
     * @param category   Analysis category (Animal health vs Milk problems).
     * @param userNote   Optional user context, notes, or specific symptoms observed.
     * @return CompletableFuture completing with the AI vision analysis text.
     */
    CompletableFuture<String> analyzeImageAsync(byte[] imageBytes, String mimeType, ImageAnalysisCategory category, String userNote);

    /**
     * Dynamically sets or updates the Gemini API key at runtime.
     *
     * @param apiKey The Gemini API key.
     */
    void setGeminiApiKey(String apiKey);

    /**
     * Dynamically sets or updates the OpenAI API key at runtime.
     *
     * @param apiKey The OpenAI API key.
     */
    void setOpenAiApiKey(String apiKey);

    /**
     * Clears configured API keys and reverts to offline fallback mode.
     */
    void clearApiKeys();

    /**
     * Saves the configured API key to a local properties file for persistence across application restarts.
     *
     * @param provider "GEMINI" or "OPENAI".
     * @param apiKey   The API key string.
     * @return true if successfully saved.
     */
    boolean persistApiKey(String provider, String apiKey);

    /**
     * Reloads configuration from environment variables, system properties, and local config files.
     */
    void reloadConfiguration();
}
