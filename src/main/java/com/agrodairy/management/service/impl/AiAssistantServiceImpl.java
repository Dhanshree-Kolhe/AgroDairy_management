package com.agrodairy.management.service.impl;

import com.agrodairy.management.model.enums.AiConnectionStatus;
import com.agrodairy.management.model.enums.ImageAnalysisCategory;
import com.agrodairy.management.service.AiAssistantService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

/**
 * Enterprise implementation of AiAssistantService supporting Google Gemini
 * Vision (default/recommended)
 * and OpenAI Vision APIs via standard Java 17 HttpClient.
 *
 * Uses currently supported Gemini vision models (defaulting to
 * gemini-2.5-flash) and avoids retired models.
 * Accurately tracks connection state to avoid claiming the service is connected
 * when it is merely configured.
 *
 * Supports configuration via:
 * 1. Environment variables (GEMINI_API_KEY, OPENAI_API_KEY, GEMINI_MODEL)
 * 2. JVM System properties (-DGEMINI_API_KEY=..., -DOPENAI_API_KEY=...,
 * -DGEMINI_MODEL=...)
 * 3. Local properties file (ai_config.properties or .env)
 * 4. Runtime interactive setting in the application UI
 */
public class AiAssistantServiceImpl implements AiAssistantService {

    private static final Logger logger = LoggerFactory.getLogger(AiAssistantServiceImpl.class);

    public static final String DEFAULT_GEMINI_MODEL = "gemini-2.5-flash";
    public static final String FALLBACK_GEMINI_MODEL = "gemini-2.0-flash";
    public static final String DEFAULT_OPENAI_MODEL = "gpt-4o-mini";

    private static final String GEMINI_API_BASE = "https://generativelanguage.googleapis.com/v1beta/models/";
    private static final String GEMINI_MODELS_URL = "https://generativelanguage.googleapis.com/v1beta/models";
    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String OPENAI_MODELS_URL = "https://api.openai.com/v1/models";

    private static final String CONFIG_FILE_NAME = "ai_config.properties";
    private static final int MAX_IMAGE_SIZE_BYTES = 10 * 1024 * 1024; // 10 MB limit
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(15);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(35);
    private static final int MAX_RETRIES = 2; // Up to 2 retries (3 attempts total)
    private static final long INITIAL_RETRY_DELAY_MS = 1000L;

    private String geminiApiKey;
    private String openAiApiKey;
    private String geminiModel = DEFAULT_GEMINI_MODEL;
    private String openAiModel = DEFAULT_OPENAI_MODEL;

    private AiConnectionStatus connectionStatus = AiConnectionStatus.UNCONFIGURED;
    private String statusDetails = "No API keys configured";

    private final HttpClient httpClient;

    public AiAssistantServiceImpl() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build();
        reloadConfiguration();
    }

    public AiAssistantServiceImpl(String geminiApiKey, String openAiApiKey) {
        this(geminiApiKey, openAiApiKey, DEFAULT_GEMINI_MODEL);
    }

    public AiAssistantServiceImpl(String geminiApiKey, String openAiApiKey, String geminiModel) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build();
        this.geminiApiKey = sanitize(geminiApiKey);
        this.openAiApiKey = sanitize(openAiApiKey);
        setGeminiModel(geminiModel);
        updateInitialStatus();
    }

    private void updateInitialStatus() {
        if (isAiConfigured()) {
            this.connectionStatus = AiConnectionStatus.CONFIGURED;
            this.statusDetails = "API key configured. Ready to invoke or test.";
        } else {
            this.connectionStatus = AiConnectionStatus.UNCONFIGURED;
            this.statusDetails = "No API keys configured";
        }
    }

    @Override
    public synchronized void reloadConfiguration() {
        // 1. Check environment variables
        this.geminiApiKey = sanitize(System.getenv("GEMINI_API_KEY"));
        this.openAiApiKey = sanitize(System.getenv("OPENAI_API_KEY"));

        // 2. Check JVM system properties
        if (this.geminiApiKey == null) {
            this.geminiApiKey = sanitize(System.getProperty("GEMINI_API_KEY"));
        }
        if (this.openAiApiKey == null) {
            this.openAiApiKey = sanitize(System.getProperty("OPENAI_API_KEY"));
        }

        // 3. Check local ai_config.properties file
        File configFile = new File(CONFIG_FILE_NAME);
        if (configFile.exists() && configFile.isFile()) {
            Properties props = new Properties();
            try (FileInputStream fis = new FileInputStream(configFile)) {
                props.load(fis);
                if (this.geminiApiKey == null) {
                    this.geminiApiKey = sanitize(props.getProperty("GEMINI_API_KEY"));
                }
                if (this.openAiApiKey == null) {
                    this.openAiApiKey = sanitize(props.getProperty("OPENAI_API_KEY"));
                }
                String customGeminiModel = props.getProperty("GEMINI_MODEL");
                if (customGeminiModel != null && !customGeminiModel.isBlank()) {
                    setGeminiModel(customGeminiModel);
                }
            } catch (IOException e) {
                logger.warn("Could not read local {}", CONFIG_FILE_NAME, e);
            }
        }

        // 4. Check local .env file
        File envFile = new File(".env");
        if (envFile.exists() && envFile.isFile()) {
            try {
                for (String line : java.nio.file.Files.readAllLines(envFile.toPath())) {
                    String trimmed = line.trim();
                    if (trimmed.startsWith("#") || !trimmed.contains("="))
                        continue;
                    int eqIdx = trimmed.indexOf('=');
                    String key = trimmed.substring(0, eqIdx).trim();
                    String val = trimmed.substring(eqIdx + 1).trim();
                    if ("GEMINI_API_KEY".equalsIgnoreCase(key) && this.geminiApiKey == null) {
                        this.geminiApiKey = sanitize(val);
                    } else if ("OPENAI_API_KEY".equalsIgnoreCase(key) && this.openAiApiKey == null) {
                        this.openAiApiKey = sanitize(val);
                    } else if ("GEMINI_MODEL".equalsIgnoreCase(key)) {
                        setGeminiModel(val);
                    }
                }
            } catch (Exception ignored) {
            }
        }

        // Check GEMINI_MODEL overrides
        String envGeminiModel = System.getenv("GEMINI_MODEL");
        if (envGeminiModel != null && !envGeminiModel.isBlank()) {
            setGeminiModel(envGeminiModel);
        }
        String propGeminiModel = System.getProperty("GEMINI_MODEL");
        if (propGeminiModel != null && !propGeminiModel.isBlank()) {
            setGeminiModel(propGeminiModel);
        }

        updateInitialStatus();
    }

    private static String sanitize(String key) {
        if (key == null)
            return null;
        String trimmed = key.trim();
        // Remove enclosing quotes if user passed "AIzaSy..." in shell
        if ((trimmed.startsWith("\"") && trimmed.endsWith("\"")) ||
                (trimmed.startsWith("'") && trimmed.endsWith("'"))) {
            if (trimmed.length() >= 2) {
                trimmed = trimmed.substring(1, trimmed.length() - 1).trim();
            }
        }
        return trimmed.isEmpty() ? null : trimmed;
    }

    @Override
    public synchronized boolean isAiConfigured() {
        return geminiApiKey != null || openAiApiKey != null;
    }

    @Override
    public synchronized AiConnectionStatus getConnectionStatus() {
        return connectionStatus;
    }

    @Override
    public synchronized String getStatusDetails() {
        return statusDetails;
    }

    @Override
    public synchronized String getActiveProviderName() {
        if (geminiApiKey != null) {
            return "Google Gemini Vision (" + geminiModel + ")";
        } else if (openAiApiKey != null) {
            return "OpenAI Vision (" + openAiModel + ")";
        } else {
            return "Unconfigured (Offline Fallback Mode)";
        }
    }

    @Override
    public synchronized String getGeminiModel() {
        return geminiModel;
    }

    @Override
    public synchronized void setGeminiModel(String model) {
        if (model == null || model.isBlank()) {
            this.geminiModel = DEFAULT_GEMINI_MODEL;
            return;
        }
        String clean = sanitize(model);
        if (clean.startsWith("models/")) {
            clean = clean.substring("models/".length()).trim();
        }
        // Automatically migrate deprecated, non-existent, or overloaded models to supported default
        if (clean.equals("gemini-1.5-flash") || clean.contains("1.5-flash") ||
                clean.equals("gemini-2.5flash") || clean.equals("gemini-3.8-flash") ||
                clean.equals("gemini-3.8flash")) {
            logger.info("Migrated retired/overloaded model identifier '{}' to active supported model '{}'", clean,
                    DEFAULT_GEMINI_MODEL);
            this.geminiModel = DEFAULT_GEMINI_MODEL;
        } else {
            this.geminiModel = clean;
        }
    }

    @Override
    public synchronized void setGeminiApiKey(String apiKey) {
        this.geminiApiKey = sanitize(apiKey);
        updateInitialStatus();
    }

    @Override
    public synchronized void setOpenAiApiKey(String apiKey) {
        this.openAiApiKey = sanitize(apiKey);
        updateInitialStatus();
    }

    @Override
    public synchronized void clearApiKeys() {
        this.geminiApiKey = null;
        this.openAiApiKey = null;
        this.connectionStatus = AiConnectionStatus.UNCONFIGURED;
        this.statusDetails = "No API keys configured";
    }

    @Override
    public synchronized boolean persistApiKey(String provider, String apiKey) {
        String sanitizedKey = sanitize(apiKey);
        if (sanitizedKey == null)
            return false;

        Properties props = new Properties();
        File configFile = new File(CONFIG_FILE_NAME);
        if (configFile.exists()) {
            try (FileInputStream fis = new FileInputStream(configFile)) {
                props.load(fis);
            } catch (IOException ignored) {
            }
        }

        if ("OPENAI".equalsIgnoreCase(provider)) {
            props.setProperty("OPENAI_API_KEY", sanitizedKey);
            this.openAiApiKey = sanitizedKey;
        } else {
            props.setProperty("GEMINI_API_KEY", sanitizedKey);
            props.setProperty("GEMINI_MODEL", this.geminiModel);
            this.geminiApiKey = sanitizedKey;
        }

        try (FileOutputStream fos = new FileOutputStream(configFile)) {
            props.store(fos, "AgroDairy AI Assistant API Configuration");
            this.connectionStatus = AiConnectionStatus.CONFIGURED;
            this.statusDetails = "API key saved. Connection untested.";
            return true;
        } catch (IOException e) {
            logger.error("Failed to persist API key to {}", CONFIG_FILE_NAME, e);
            return false;
        }
    }

    @Override
    public CompletableFuture<Boolean> testConnectionAsync() {
        if (!isAiConfigured()) {
            this.connectionStatus = AiConnectionStatus.UNCONFIGURED;
            this.statusDetails = "No API key configured";
            return CompletableFuture.completedFuture(false);
        }

        if (geminiApiKey != null) {
            String testUrl = GEMINI_MODELS_URL;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(testUrl))
                    .timeout(Duration.ofSeconds(10))
                    .header("x-goog-api-key", geminiApiKey)
                    .GET()
                    .build();

            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                    .thenApply(response -> {
                        if (response.statusCode() == 200) {
                            String body = response.body();
                            if (body != null && (body.contains("\"name\": \"models/" + geminiModel + "\"")
                                    || body.contains("models/" + geminiModel))) {
                                this.connectionStatus = AiConnectionStatus.CONNECTED;
                                this.statusDetails = "Connection verified with Google Gemini API (" + geminiModel + ").";
                                return true;
                            } else if (body != null) {
                                // Configured model was not found in supported models list - find best available
                                String fallback = null;
                                if (body.contains("models/" + DEFAULT_GEMINI_MODEL)) {
                                    fallback = DEFAULT_GEMINI_MODEL;
                                } else if (body.contains("models/" + FALLBACK_GEMINI_MODEL)) {
                                    fallback = FALLBACK_GEMINI_MODEL;
                                }

                                if (fallback != null) {
                                    logger.info("Configured model '{}' not found in available models; switching to supported '{}'",
                                            geminiModel, fallback);
                                    this.geminiModel = fallback;
                                    this.connectionStatus = AiConnectionStatus.CONNECTED;
                                    this.statusDetails = "Connection verified with Google Gemini API (" + geminiModel + ").";
                                    return true;
                                } else {
                                    this.connectionStatus = AiConnectionStatus.ERROR;
                                    this.statusDetails = "Verification warning: Model '" + geminiModel + "' not found in available models.";
                                    return false;
                                }
                            } else {
                                this.connectionStatus = AiConnectionStatus.CONNECTED;
                                this.statusDetails = "Connection verified with Google Gemini API (" + geminiModel + ").";
                                return true;
                            }
                        } else {
                            this.connectionStatus = AiConnectionStatus.ERROR;
                            this.statusDetails = "Verification failed (HTTP " + response.statusCode() + "): " +
                                    extractJsonStringField(response.body(), "message");
                            return false;
                        }
                    })
                    .exceptionally(ex -> {
                        this.connectionStatus = AiConnectionStatus.ERROR;
                        this.statusDetails = "Network error: " + ex.getMessage();
                        return false;
                    });
        } else {
            String testUrl = OPENAI_MODELS_URL;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(testUrl))
                    .timeout(Duration.ofSeconds(10))
                    .header("Authorization", "Bearer " + openAiApiKey)
                    .GET()
                    .build();

            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                    .thenApply(response -> {
                        if (response.statusCode() == 200) {
                            this.connectionStatus = AiConnectionStatus.CONNECTED;
                            this.statusDetails = "Connection verified with OpenAI API (" + openAiModel + ").";
                            return true;
                        } else {
                            this.connectionStatus = AiConnectionStatus.ERROR;
                            this.statusDetails = "Verification failed (HTTP " + response.statusCode() + "): " +
                                    extractJsonStringField(response.body(), "message");
                            return false;
                        }
                    })
                    .exceptionally(ex -> {
                        this.connectionStatus = AiConnectionStatus.ERROR;
                        this.statusDetails = "Network error: " + ex.getMessage();
                        return false;
                    });
        }
    }

    @Override
    public String getConfigurationInstructions() {
        return "⚙️ AI Vision Assistant Setup & Activation Guide:\n\n" +
                "To enable genuine real-time image analysis and AI conversational responses:\n\n" +
                "1. OBTAIN AN API KEY (FREE & INSTANT):\n" +
                "   • Google Gemini Vision (Recommended):\n" +
                "     Visit Google AI Studio: https://aistudio.google.com\n" +
                "     Sign in with your Google account and click 'Create API Key' (free tier available).\n" +
                "   • OpenAI Vision (Alternative):\n" +
                "     Visit OpenAI Platform: https://platform.openai.com/api-keys\n\n" +
                "2. CONFIGURE YOUR API KEY (CHOOSE ONE METHOD):\n\n" +
                "   METHOD A — IN-APP DIRECT ACTIVATION (EASIEST):\n" +
                "   Click '⚙️ AI Setup Guide' on this screen, paste your key into the input field,\n" +
                "   and click 'Save & Activate'. It takes effect immediately without restarting!\n\n" +
                "   METHOD B — WINDOWS POWERSHELL:\n" +
                "   Run in your terminal before launching the application:\n" +
                "     $env:GEMINI_API_KEY=\"your_actual_api_key_here\"\n" +
                "     mvn javafx:run\n\n" +
                "   METHOD C — WINDOWS COMMAND PROMPT (CMD):\n" +
                "     set GEMINI_API_KEY=your_actual_api_key_here\n" +
                "     mvn javafx:run\n\n" +
                "   METHOD D — LOCAL CONFIG FILE:\n" +
                "   Create a file named 'ai_config.properties' in the project folder with:\n" +
                "     GEMINI_API_KEY=your_actual_api_key_here\n" +
                "     GEMINI_MODEL=" + DEFAULT_GEMINI_MODEL + "\n\n" +
                "3. ACTIVE MODEL IDENTIFIERS:\n" +
                "   • Active Supported Model: " + geminiModel + " (replaces deprecated gemini-1.5-flash)\n" +
                "   • Custom model override supported via GEMINI_MODEL environment variable.\n\n" +
                "4. OFFLINE SAFETY:\n" +
                "   While unconfigured, text inquiries safely use the built-in offline Dairy Knowledge Base.\n" +
                "   Genuine image inspection requires an active vision model. AgroDairy never fabricates fake analysis.";
    }

    @Override
    public CompletableFuture<String> askAiAsync(String prompt) {
        if (prompt == null || prompt.trim().isEmpty()) {
            return CompletableFuture.completedFuture("Please enter a question or topic to inquire.");
        }

        if (!isAiConfigured()) {
            return CompletableFuture.completedFuture(
                    "⚠️ Live AI is currently unconfigured. Set the GEMINI_API_KEY or OPENAI_API_KEY environment variable to enable live generative AI.\n\n"
                            +
                            getConfigurationInstructions());
        }

        String systemInstruction = "You are the AgroDairy Smart Assistant, an expert agricultural consultant specializing in dairy farming, cattle care, milk handling, boiling, curd setting, storage, and customer support. "
                +
                "Provide accurate, structured, helpful, and concise answers with clear bullet points. Emphasize animal welfare and dairy hygiene. "
                +
                "Always include a reminder that serious cattle illnesses require consultation with a licensed veterinarian.";

        if (geminiApiKey != null) {
            return callGeminiText(systemInstruction, prompt.trim());
        } else {
            return callOpenAiText(systemInstruction, prompt.trim());
        }
    }

    @Override
    public CompletableFuture<String> analyzeImageAsync(byte[] imageBytes, String mimeType,
            ImageAnalysisCategory category, String userNote) {
        if (imageBytes == null || imageBytes.length == 0) {
            return CompletableFuture
                    .completedFuture("⚠️ Error: No image data provided. Please upload a valid image file.");
        }

        if (imageBytes.length > MAX_IMAGE_SIZE_BYTES) {
            return CompletableFuture.completedFuture(
                    "⚠️ Error: Image file size exceeds the 10 MB limit. Please select a smaller photo.");
        }

        String normalizedMime = normalizeMimeType(mimeType);
        if (normalizedMime == null) {
            return CompletableFuture.completedFuture(
                    "⚠️ Error: Unsupported image format. Only JPG, JPEG, and PNG images are supported.");
        }

        if (!isAiConfigured()) {
            return CompletableFuture.completedFuture(
                    "⚠️ AI Vision Analysis is unavailable because no AI API key is configured.\n\n" +
                            "To enable genuine vision inspection of cattle symptoms and milk quality, please configure the GEMINI_API_KEY or OPENAI_API_KEY.\n\n"
                            +
                            "Why an API key is required: Genuine vision models evaluate complex visual features such as skin lesions, udder inflammation, or milk curdling. "
                            +
                            "AgroDairy never presents fake or mock image analysis as real AI.\n\n" +
                            getConfigurationInstructions());
        }

        String prompt = buildVisionPrompt(category, userNote);
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);

        if (geminiApiKey != null) {
            return callGeminiVision(prompt, normalizedMime, base64Image);
        } else {
            return callOpenAiVision(prompt, normalizedMime, base64Image);
        }
    }

    private String buildVisionPrompt(ImageAnalysisCategory category, String userNote) {
        StringBuilder sb = new StringBuilder();
        if (category == ImageAnalysisCategory.ANIMAL_HEALTH) {
            sb.append(
                    "You are an expert veterinary agricultural dairy consultant. The user has uploaded a photo of cattle (cow/buffalo) for preliminary visual observation.\n\n")
                    .append("MANDATORY MEDICAL DIRECTIVE & DISCLAIMER:\n")
                    .append("• You MUST NOT claim or offer a definitive clinical diagnosis.\n")
                    .append("• You MUST state clearly that photographic observation is purely informational and cannot replace an in-person physical examination by a licensed veterinarian.\n\n")
                    .append("Please provide a structured response with the following exact headers:\n\n")
                    .append("1. 🔍 VISIBLE OBSERVATIONS:\n")
                    .append("Describe observable physical characteristics (e.g. skin lesions, coat condition, posture, udder appearance, ocular/nasal signs, body condition). If visual quality is limited or ambiguous, state so.\n\n")
                    .append("2. ⚠️ POTENTIAL HEALTH CONCERNS (NON-DIAGNOSTIC):\n")
                    .append("Discuss possible health concerns consistent with visible signs (e.g., mastitis indicators, lumpy skin signs, ringworm, ectoparasites, foot rot, heat stress) for discussion with a vet.\n\n")
                    .append("3. 🩺 RECOMMENDED IMMEDIATE ACTION & CARE:\n")
                    .append("Actionable supportive care steps (isolation if contagious suspected, fresh water, resting surface, temperature checking, recording progression).\n\n")
                    .append("4. 🚨 VETERINARY CONSULTATION NOTICE:\n")
                    .append("Explicitly instruct the dairy farmer/owner to seek immediate professional veterinary care.");
        } else {
            sb.append(
                    "You are a certified dairy technologist and milk quality assurance specialist. The user has uploaded a photo of a milk sample for preliminary visual assessment.\n\n")
                    .append("MANDATORY FOOD SAFETY DIRECTIVE & DISCLAIMER:\n")
                    .append("• You MUST NOT certify or claim that milk is safe, pure, or fit for consumption from a photograph alone.\n")
                    .append("• Bacterial pathogens (e.g., Salmonella, E. coli, Listeria), chemical adulterants, and acidity (pH) CANNOT be verified visually.\n\n")
                    .append("Please provide a structured response with the following exact headers:\n\n")
                    .append("1. 🔍 VISIBLE PHYSICAL CHARACTERISTICS:\n")
                    .append("Describe visual attributes (color tint, texture, curdling, watery separation, clumping, froth, foreign particulate matter).\n\n")
                    .append("2. ⚠️ POTENTIAL CAUSES / QUALITY CONCERNS:\n")
                    .append("Discuss possible issues (e.g., natural lactic acid souring, mastitic clots, colostrum mixing, heat instability, container contamination, delayed chilling).\n\n")
                    .append("3. 🔬 LIMITATIONS OF VISUAL INSPECTION:\n")
                    .append("Highlight what visual inspection cannot determine: microbial count, somatic cell count, chemical purity, and hygiene safety require laboratory or rapid testing.\n\n")
                    .append("4. 📋 RECOMMENDED ACTIONS & TESTING:\n")
                    .append("Practical safe handling instructions (e.g., spoon boiling test, alcohol test, lactometer testing, do not consume if foul odor or clotted, sanitize utensils).");
        }

        if (userNote != null && !userNote.trim().isEmpty()) {
            sb.append("\n\nADDITIONAL USER CONTEXT & NOTES PROVIDED:\n\"")
                    .append(userNote.trim())
                    .append("\"");
        }

        return sb.toString();
    }

    private String normalizeMimeType(String mime) {
        if (mime == null)
            return "image/jpeg";
        String lower = mime.trim().toLowerCase();
        if (lower.contains("png"))
            return "image/png";
        if (lower.contains("jpeg") || lower.contains("jpg"))
            return "image/jpeg";
        if (lower.contains("webp"))
            return "image/webp";
        return null;
    }

    // =========================================================================
    // GOOGLE GEMINI API INTEGRATION
    // =========================================================================

    private CompletableFuture<String> callGeminiText(String systemInstruction, String userPrompt) {
        String url = GEMINI_API_BASE + geminiModel + ":generateContent";
        String jsonPayload = "{"
                + "\"contents\": [{"
                + "\"parts\": ["
                + "{\"text\": \"" + escapeJson(systemInstruction + "\n\nUser Question: " + userPrompt) + "\"}"
                + "]"
                + "}]"
                + "}";

        return executeHttpPost(url, jsonPayload, null)
                .thenCompose(response -> {
                    if ((response.statusCode == 503 || response.statusCode == 404)
                            && !geminiModel.equals(FALLBACK_GEMINI_MODEL)) {
                        logger.warn("Gemini model '{}' returned HTTP {}. Retrying with fallback model '{}'...",
                                geminiModel, response.statusCode, FALLBACK_GEMINI_MODEL);
                        String fallbackUrl = GEMINI_API_BASE + FALLBACK_GEMINI_MODEL + ":generateContent";
                        return executeHttpPost(fallbackUrl, jsonPayload, null)
                                .thenApply(fallbackRes -> {
                                    if (fallbackRes.statusCode == 200) {
                                        this.geminiModel = FALLBACK_GEMINI_MODEL;
                                        return parseGeminiResponse(fallbackRes);
                                    }
                                    return parseGeminiResponse(response);
                                });
                    }
                    return CompletableFuture.completedFuture(parseGeminiResponse(response));
                });
    }

    private CompletableFuture<String> callGeminiVision(String prompt, String mimeType, String base64Image) {
        String url = GEMINI_API_BASE + geminiModel + ":generateContent";
        String jsonPayload = "{"
                + "\"contents\": [{"
                + "\"parts\": ["
                + "{\"text\": \"" + escapeJson(prompt) + "\"},"
                + "{\"inlineData\": {"
                + "\"mimeType\": \"" + escapeJson(mimeType) + "\","
                + "\"data\": \"" + base64Image + "\""
                + "}}"
                + "]"
                + "}]"
                + "}";

        return executeHttpPost(url, jsonPayload, null)
                .thenCompose(response -> {
                    if ((response.statusCode == 503 || response.statusCode == 404)
                            && !geminiModel.equals(FALLBACK_GEMINI_MODEL)) {
                        logger.warn("Gemini vision model '{}' returned HTTP {}. Retrying with fallback model '{}'...",
                                geminiModel, response.statusCode, FALLBACK_GEMINI_MODEL);
                        String fallbackUrl = GEMINI_API_BASE + FALLBACK_GEMINI_MODEL + ":generateContent";
                        return executeHttpPost(fallbackUrl, jsonPayload, null)
                                .thenApply(fallbackRes -> {
                                    if (fallbackRes.statusCode == 200) {
                                        this.geminiModel = FALLBACK_GEMINI_MODEL;
                                        return parseGeminiResponse(fallbackRes);
                                    }
                                    return parseGeminiResponse(response);
                                });
                    }
                    return CompletableFuture.completedFuture(parseGeminiResponse(response));
                });
    }

    public String parseGeminiResponse(HttpResponseWrapper response) {
        if (response.statusCode != 200) {
            this.connectionStatus = AiConnectionStatus.ERROR;
            this.statusDetails = "Gemini API error (HTTP " + response.statusCode + ")";
            return formatHttpError("Google Gemini", response.statusCode, response.body);
        }

        String body = response.body;
        if (body == null || body.isBlank()) {
            this.connectionStatus = AiConnectionStatus.ERROR;
            this.statusDetails = "Gemini returned empty response body";
            return "⚠️ Gemini service returned an empty response.";
        }

        // Check for error payload
        if (body.contains("\"error\"")) {
            this.connectionStatus = AiConnectionStatus.ERROR;
            String msg = extractJsonStringField(body, "message");
            if (msg == null) msg = "Unknown error in API payload";
            if (geminiApiKey != null && msg.contains(geminiApiKey)) {
                msg = msg.replace(geminiApiKey, "[REDACTED_API_KEY]");
            }
            this.statusDetails = "Gemini payload error: " + msg;
            return "⚠️ Gemini API Error: " + msg;
        }

        // Resilient multi-part extractor
        String extracted = extractGeminiText(body);
        if (extracted != null && !extracted.isBlank()) {
            this.connectionStatus = AiConnectionStatus.CONNECTED;
            this.statusDetails = "Connected & verified with Gemini (" + geminiModel + ")";
            return extracted;
        }

        // Check if blocked by safety filters
        if (body.contains("BLOCK") || body.contains("SAFETY")) {
            return "⚠️ The analysis could not be completed because the image or prompt was flagged by the safety filter. Please ensure the image is clear and appropriate.";
        }

        this.connectionStatus = AiConnectionStatus.CONNECTED;
        this.statusDetails = "Connected (unformatted payload)";
        return "Received unformatted response from Gemini:\n" + body;
    }

    public static String extractGeminiText(String json) {
        if (json == null || json.isBlank())
            return null;

        StringBuilder combined = new StringBuilder();
        int searchIdx = 0;
        while (true) {
            int keyIdx = json.indexOf("\"text\"", searchIdx);
            if (keyIdx == -1)
                break;

            int colonIdx = json.indexOf(':', keyIdx + 6);
            if (colonIdx == -1)
                break;

            int quoteIdx = json.indexOf('"', colonIdx + 1);
            if (quoteIdx == -1)
                break;

            String partText = extractJsonStringLiteral(json, quoteIdx + 1);
            if (!partText.isEmpty()) {
                if (combined.length() > 0)
                    combined.append("\n");
                combined.append(partText);
            }
            searchIdx = quoteIdx + 1;
        }

        return combined.length() > 0 ? combined.toString() : null;
    }

    // =========================================================================
    // OPENAI API INTEGRATION
    // =========================================================================

    private CompletableFuture<String> callOpenAiText(String systemInstruction, String userPrompt) {
        String url = OPENAI_API_URL;
        String jsonPayload = "{"
                + "\"model\": \"" + openAiModel + "\","
                + "\"messages\": ["
                + "{\"role\": \"system\", \"content\": \"" + escapeJson(systemInstruction) + "\"},"
                + "{\"role\": \"user\", \"content\": \"" + escapeJson(userPrompt) + "\"}"
                + "]"
                + "}";

        return executeHttpPost(url, jsonPayload, "Bearer " + openAiApiKey)
                .thenApply(this::parseOpenAiResponse);
    }

    private CompletableFuture<String> callOpenAiVision(String prompt, String mimeType, String base64Image) {
        String url = OPENAI_API_URL;
        String dataUrl = "data:" + mimeType + ";base64," + base64Image;

        String jsonPayload = "{"
                + "\"model\": \"" + openAiModel + "\","
                + "\"messages\": ["
                + "{\"role\": \"user\", \"content\": ["
                + "{\"type\": \"text\", \"text\": \"" + escapeJson(prompt) + "\"},"
                + "{\"type\": \"image_url\", \"image_url\": {\"url\": \"" + dataUrl + "\"}}"
                + "]}"
                + "]"
                + "}";

        return executeHttpPost(url, jsonPayload, "Bearer " + openAiApiKey)
                .thenApply(this::parseOpenAiResponse);
    }

    public String parseOpenAiResponse(HttpResponseWrapper response) {
        if (response.statusCode != 200) {
            this.connectionStatus = AiConnectionStatus.ERROR;
            this.statusDetails = "OpenAI API error (HTTP " + response.statusCode + ")";
            return formatHttpError("OpenAI", response.statusCode, response.body);
        }

        String body = response.body;
        if (body == null || body.isBlank()) {
            this.connectionStatus = AiConnectionStatus.ERROR;
            this.statusDetails = "OpenAI returned empty response body";
            return "⚠️ OpenAI service returned an empty response.";
        }

        if (body.contains("\"error\"")) {
            this.connectionStatus = AiConnectionStatus.ERROR;
            String msg = extractJsonStringField(body, "message");
            this.statusDetails = "OpenAI payload error: " + (msg != null ? msg : "Unknown");
            return "⚠️ OpenAI API Error: " + (msg != null ? msg : "Unknown error in API payload");
        }

        String content = extractJsonStringField(body, "content");
        if (content != null && !content.isBlank()) {
            this.connectionStatus = AiConnectionStatus.CONNECTED;
            this.statusDetails = "Connected & verified with OpenAI (" + openAiModel + ")";
            return content;
        }

        this.connectionStatus = AiConnectionStatus.CONNECTED;
        this.statusDetails = "Connected (unformatted payload)";
        return "Received unformatted response from OpenAI:\n" + body;
    }

    // =========================================================================
    // HTTP CLIENT HELPER & ERROR FORMATTING
    // =========================================================================

    private CompletableFuture<HttpResponseWrapper> executeHttpPost(String url, String jsonBody, String authHeader) {
        return executeHttpPostWithRetry(url, jsonBody, authHeader, 0);
    }

    private CompletableFuture<HttpResponseWrapper> executeHttpPostWithRetry(
            String url, String jsonBody, String authHeader, int attempt) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Content-Type", "application/json");

            if (authHeader != null) {
                builder.header("Authorization", authHeader);
            }
            if (geminiApiKey != null && url.contains("generativelanguage.googleapis.com")) {
                builder.header("x-goog-api-key", geminiApiKey);
            }

            HttpRequest request = builder.POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                    .build();

            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                    .thenCompose(res -> {
                        int code = res.statusCode();
                        if ((code == 503 || code == 429) && attempt < MAX_RETRIES) {
                            long delayMs = INITIAL_RETRY_DELAY_MS * (attempt + 1);
                            logger.warn("AI service returned HTTP {}. Retrying attempt {}/{} in {} ms...",
                                    code, attempt + 1, MAX_RETRIES, delayMs);
                            CompletableFuture<HttpResponseWrapper> delayedFuture = new CompletableFuture<>();
                            CompletableFuture.delayedExecutor(delayMs, java.util.concurrent.TimeUnit.MILLISECONDS)
                                    .execute(() -> executeHttpPostWithRetry(url, jsonBody, authHeader, attempt + 1)
                                            .whenComplete((retryRes, retryEx) -> {
                                                if (retryEx != null) {
                                                    delayedFuture.completeExceptionally(retryEx);
                                                } else {
                                                    delayedFuture.complete(retryRes);
                                                }
                                            }));
                            return delayedFuture;
                        }
                        return CompletableFuture.completedFuture(new HttpResponseWrapper(code, res.body()));
                    })
                    .exceptionallyCompose(ex -> {
                        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                        if ((cause instanceof HttpTimeoutException || cause instanceof IOException) && attempt < MAX_RETRIES) {
                            long delayMs = INITIAL_RETRY_DELAY_MS * (attempt + 1);
                            logger.warn("AI service network issue ({}). Retrying attempt {}/{} in {} ms...",
                                    cause.getClass().getSimpleName(), attempt + 1, MAX_RETRIES, delayMs);
                            CompletableFuture<HttpResponseWrapper> delayedFuture = new CompletableFuture<>();
                            CompletableFuture.delayedExecutor(delayMs, java.util.concurrent.TimeUnit.MILLISECONDS)
                                    .execute(() -> executeHttpPostWithRetry(url, jsonBody, authHeader, attempt + 1)
                                            .whenComplete((retryRes, retryEx) -> {
                                                if (retryEx != null) {
                                                    delayedFuture.completeExceptionally(retryEx);
                                                } else {
                                                    delayedFuture.complete(retryRes);
                                                }
                                            }));
                            return delayedFuture;
                        }
                        logger.error("HTTP request to AI service failed after attempts", ex);
                        this.connectionStatus = AiConnectionStatus.ERROR;
                        this.statusDetails = "Network communication error: " + cause.getMessage();
                        if (cause instanceof HttpTimeoutException) {
                            return CompletableFuture.completedFuture(new HttpResponseWrapper(408, "Request timed out after " + REQUEST_TIMEOUT.toSeconds()
                                    + " seconds. Please verify your internet connection."));
                        }
                        return CompletableFuture.completedFuture(new HttpResponseWrapper(500, "Network communication error: " + cause.getMessage()));
                    });
        } catch (Exception e) {
            logger.error("Error creating HTTP request to AI service", e);
            this.connectionStatus = AiConnectionStatus.ERROR;
            this.statusDetails = "Failed to initialize HTTP request: " + e.getMessage();
            return CompletableFuture.completedFuture(
                    new HttpResponseWrapper(500, "Failed to initialize HTTP request: " + e.getMessage()));
        }
    }

    private String formatHttpError(String provider, int statusCode, String body) {
        String extractedMsg = extractJsonStringField(body, "message");
        if (extractedMsg == null) {
            extractedMsg = body;
        }

        // Mask any potential echoed key in error message
        if (geminiApiKey != null && extractedMsg != null && extractedMsg.contains(geminiApiKey)) {
            extractedMsg = extractedMsg.replace(geminiApiKey, "[REDACTED_API_KEY]");
        }
        if (openAiApiKey != null && extractedMsg != null && extractedMsg.contains(openAiApiKey)) {
            extractedMsg = extractedMsg.replace(openAiApiKey, "[REDACTED_API_KEY]");
        }

        String activeModel = provider.contains("Gemini") ? geminiModel : openAiModel;

        return switch (statusCode) {
            case 400 -> "⚠️ Bad Request (HTTP 400): The " + provider + " API rejected the request format.\n" +
                    "Details: " + extractedMsg;
            case 401 -> "⚠️ Authentication Error (HTTP 401): The provided " + provider
                    + " API key is invalid or unauthorized.\n" +
                    "Please verify your key at Google AI Studio or OpenAI dashboard.";
            case 403 -> "⚠️ Access Forbidden (HTTP 403): The " + provider
                    + " API key does not have permission to access the vision model.\n" +
                    "Details: " + extractedMsg;
            case 404 -> "⚠️ Model Not Found (HTTP 404): The model '" + activeModel + "' was not found on " + provider
                    + " for endpoint 'v1beta'.\n" +
                    "Details: " + extractedMsg + "\n" +
                    "Note: Deprecated models like gemini-1.5-flash are retired. Recommended active models: '" + DEFAULT_GEMINI_MODEL + "' or '" + FALLBACK_GEMINI_MODEL + "'.";
            case 408 ->
                "⚠️ Request Timeout (HTTP 408): The AI service did not respond within the time limit. Please check your network.";
            case 429 -> "⚠️ Rate Limit Exceeded (HTTP 429): Quota or request limit reached on " + provider + ".\n" +
                    "Details: " + extractedMsg + "\n" +
                    "Please wait a few moments or check your API billing/free quota tier.";
            case 500, 502,
                    503 ->
                "⚠️ Service Unavailable (HTTP " + statusCode + "): " + provider + " servers are currently overloaded or experiencing high traffic.\n"
                        + "Details: " + extractedMsg + "\n"
                        + "Automatic retries were attempted. Please try again in a few moments, or check model status.";
            default -> "⚠️ AI Service Error (HTTP " + statusCode + "): " + extractedMsg;
        };
    }

    public static class HttpResponseWrapper {
        public final int statusCode;
        public final String body;

        public HttpResponseWrapper(int statusCode, String body) {
            this.statusCode = statusCode;
            this.body = body;
        }
    }

    // =========================================================================
    // JSON ESCAPING & STRING LITERAL PARSER
    // =========================================================================

    public static String escapeJson(String s) {
        if (s == null)
            return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < ' ') {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }

    public static String extractJsonStringField(String json, String fieldName) {
        if (json == null || fieldName == null)
            return null;
        int keyIdx = json.indexOf("\"" + fieldName + "\"");
        if (keyIdx == -1)
            return null;
        int colonIdx = json.indexOf(':', keyIdx + fieldName.length() + 2);
        if (colonIdx == -1)
            return null;
        int quoteIdx = json.indexOf('"', colonIdx + 1);
        if (quoteIdx == -1)
            return null;
        return extractJsonStringLiteral(json, quoteIdx + 1);
    }

    public static String extractJsonStringLiteral(String json, int start) {
        if (json == null || start >= json.length())
            return "";
        StringBuilder sb = new StringBuilder();
        boolean escaping = false;
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escaping) {
                switch (c) {
                    case '"' -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    case '/' -> sb.append('/');
                    case 'b' -> sb.append('\b');
                    case 'f' -> sb.append('\f');
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case 'u' -> {
                        if (i + 4 < json.length()) {
                            String hex = json.substring(i + 1, i + 5);
                            try {
                                sb.append((char) Integer.parseInt(hex, 16));
                                i += 4;
                            } catch (NumberFormatException e) {
                                sb.append('u');
                            }
                        } else {
                            sb.append('u');
                        }
                    }
                    default -> sb.append(c);
                }
                escaping = false;
            } else if (c == '\\') {
                escaping = true;
            } else if (c == '"') {
                break;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
