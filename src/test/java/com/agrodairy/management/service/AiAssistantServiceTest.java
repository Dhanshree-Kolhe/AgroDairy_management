package com.agrodairy.management.service;

import com.agrodairy.management.model.enums.ImageAnalysisCategory;
import com.agrodairy.management.service.impl.AiAssistantServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

public class AiAssistantServiceTest {

    @Test
    @DisplayName("Verify unconfigured AI returns explicit configuration guide and does not fake analysis")
    public void testUnconfiguredAiService() throws Exception {
        AiAssistantServiceImpl service = new AiAssistantServiceImpl(null, null);

        assertFalse(service.isAiConfigured(), "AI should report not configured when keys are null");
        assertEquals("Unconfigured (Offline Fallback Mode)", service.getActiveProviderName());
        assertTrue(service.getConfigurationInstructions().contains("GEMINI_API_KEY"));

        // Text query when unconfigured
        CompletableFuture<String> textFuture = service.askAiAsync("What causes milk curdling?");
        String textResult = textFuture.get();
        assertTrue(textResult.contains("GEMINI_API_KEY") || textResult.contains("Offline"),
                "Unconfigured text inquiry must instruct the user about API keys without faking responses");

        // Image analysis when unconfigured
        byte[] dummyImage = new byte[]{1, 2, 3, 4, 5};
        CompletableFuture<String> imageFuture = service.analyzeImageAsync(
                dummyImage, "image/jpeg", ImageAnalysisCategory.ANIMAL_HEALTH, "Lump on cow's neck"
        );
        String imageResult = imageFuture.get();
        assertTrue(imageResult.contains("GEMINI_API_KEY") || imageResult.contains("API key is required"),
                "Unconfigured image analysis must clearly indicate AI key requirement and never return fake diagnoses");
    }

    @Test
    @DisplayName("Verify image validation rejects invalid inputs")
    public void testImageValidation() throws Exception {
        AiAssistantServiceImpl service = new AiAssistantServiceImpl("dummy-key", null);

        // Empty bytes
        String emptyResult = service.analyzeImageAsync(new byte[0], "image/jpeg", ImageAnalysisCategory.ANIMAL_HEALTH, null).get();
        assertTrue(emptyResult.contains("Error"), "Should report error on empty image bytes");

        // Null bytes
        String nullResult = service.analyzeImageAsync(null, "image/jpeg", ImageAnalysisCategory.ANIMAL_HEALTH, null).get();
        assertTrue(nullResult.contains("Error"), "Should report error on null image bytes");

        // Unsupported format
        String formatResult = service.analyzeImageAsync(new byte[]{1, 2}, "image/gif", ImageAnalysisCategory.MILK_PROBLEMS, null).get();
        assertTrue(formatResult.contains("Unsupported image format"), "Should report error on non-JPG/PNG formats");
    }

    @Test
    @DisplayName("Verify JSON escaping and literal extraction")
    public void testJsonEscapingAndExtraction() {
        String original = "Hello \"world\"\nNew line with \\ backslash";
        String escaped = AiAssistantServiceImpl.escapeJson(original);
        assertTrue(escaped.contains("\\\"world\\\""));
        assertTrue(escaped.contains("\\n"));

        // Test Gemini response parsing with escaped characters
        String geminiJson = "{\"candidates\": [{\"content\": {\"parts\": [{\"text\": \"Visible observations:\\n- Cow has skin nodules.\\nRecommended vet visit immediately.\"}]}}]}";
        AiAssistantServiceImpl.HttpResponseWrapper res = new AiAssistantServiceImpl.HttpResponseWrapper(200, geminiJson);
        AiAssistantServiceImpl service = new AiAssistantServiceImpl("key", null);
        String parsed = service.parseGeminiResponse(res);

        assertTrue(parsed.contains("Cow has skin nodules."));
        assertTrue(parsed.contains("Recommended vet visit immediately."));

        // Test OpenAI response parsing
        String openAiJson = "{\"choices\": [{\"message\": {\"content\": \"Milk sample analysis:\\n- Yellowish color.\\nCannot certify bacterial safety.\"}}]}";
        AiAssistantServiceImpl.HttpResponseWrapper openAiRes = new AiAssistantServiceImpl.HttpResponseWrapper(200, openAiJson);
        String parsedOpenAi = service.parseOpenAiResponse(openAiRes);

        assertTrue(parsedOpenAi.contains("Yellowish color."));
        assertTrue(parsedOpenAi.contains("Cannot certify bacterial safety."));
    }

    @Test
    @DisplayName("Verify HTTP error code formatting gives clear user-facing explanations")
    public void testHttpErrorFormatting() {
        AiAssistantServiceImpl service = new AiAssistantServiceImpl("key", null);

        // 401 Unauthorized
        AiAssistantServiceImpl.HttpResponseWrapper res401 = new AiAssistantServiceImpl.HttpResponseWrapper(401, "{\"error\": {\"message\": \"API_KEY_INVALID\"}}");
        String err401 = service.parseGeminiResponse(res401);
        assertTrue(err401.contains("Authentication Error") || err401.contains("401"));

        // 429 Rate Limit
        AiAssistantServiceImpl.HttpResponseWrapper res429 = new AiAssistantServiceImpl.HttpResponseWrapper(429, "{\"error\": {\"message\": \"RESOURCE_EXHAUSTED\"}}");
        String err429 = service.parseGeminiResponse(res429);
        assertTrue(err429.contains("Rate Limit") || err429.contains("429"));
    }

    @Test
    @DisplayName("Verify dynamic key setting, quote sanitization, and clearing")
    public void testDynamicKeySettingAndSanitization() {
        AiAssistantServiceImpl service = new AiAssistantServiceImpl(null, null);
        assertFalse(service.isAiConfigured());

        // Set with outer quotes (common PowerShell artifact)
        service.setGeminiApiKey("\"AIzaSyFakeKey123\"");
        assertTrue(service.isAiConfigured());
        assertEquals("Google Gemini Vision (gemini-2.5-flash)", service.getActiveProviderName());

        // Set OpenAI key
        service.clearApiKeys();
        assertFalse(service.isAiConfigured());
        service.setOpenAiApiKey("'sk-fake-openai-key'");
        assertTrue(service.isAiConfigured());
        assertEquals("OpenAI Vision (gpt-4o-mini)", service.getActiveProviderName());

        service.clearApiKeys();
        assertFalse(service.isAiConfigured());
    }

    @Test
    @DisplayName("Verify multi-part Gemini text extraction with arbitrary whitespace")
    public void testMultiPartGeminiExtraction() {
        String json = "{\n" +
                "  \"candidates\": [{\n" +
                "    \"content\": {\n" +
                "      \"parts\": [\n" +
                "        { \"text\" : \"Section 1: Visible swelling on right flank.\" },\n" +
                "        { \"text\":\n \"Section 2: Consult vet immediately.\" }\n" +
                "      ]\n" +
                "    }\n" +
                "  }]\n" +
                "}";

        String extracted = AiAssistantServiceImpl.extractGeminiText(json);
        assertNotNull(extracted);
        assertTrue(extracted.contains("Section 1: Visible swelling on right flank."));
        assertTrue(extracted.contains("Section 2: Consult vet immediately."));
    }
}
