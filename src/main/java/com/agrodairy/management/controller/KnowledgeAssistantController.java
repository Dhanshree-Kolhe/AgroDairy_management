package com.agrodairy.management.controller;

import com.agrodairy.management.model.FAQKnowledgeBase;
import com.agrodairy.management.model.enums.ImageAnalysisCategory;
import com.agrodairy.management.service.AiAssistantService;
import com.agrodairy.management.service.KnowledgeAssistantService;
import com.agrodairy.management.service.impl.AiAssistantServiceImpl;
import com.agrodairy.management.service.impl.KnowledgeAssistantServiceImpl;
import com.agrodairy.management.util.AlertUtils;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Controller for the upgraded Dairy Knowledge Assistant & AI Vision Inspector.
 * Supports interactive AI chat, image upload, category selection, and genuine vision AI inspection
 * with graceful fallback to the local offline knowledge base.
 */
public class KnowledgeAssistantController {

    private static final Logger logger = LoggerFactory.getLogger(KnowledgeAssistantController.class);
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    // Header Controls
    @FXML private Label aiStatusBadge;
    @FXML private TabPane mainTabPane;

    // Chat Controls (Tab 1)
    @FXML private ScrollPane chatScrollPane;
    @FXML private VBox chatContainer;
    @FXML private HBox chatLoadingBox;
    @FXML private Label chatLoadingLabel;
    @FXML private TextField chatInputField;
    @FXML private Button sendBtn;

    // Vision Analysis Controls (Tab 1 Right Panel)
    @FXML private RadioButton radioAnimalHealth;
    @FXML private RadioButton radioMilkProblems;
    @FXML private Button uploadImageBtn;
    @FXML private Button removeImageBtn;
    @FXML private StackPane imagePreviewContainer;
    @FXML private VBox emptyPreviewPlaceholder;
    @FXML private ImageView imagePreviewView;
    @FXML private Label imageDetailsLabel;
    @FXML private TextField imageNotesField;
    @FXML private Button analyzeImageBtn;

    // Offline FAQ Controls (Tab 2)
    @FXML private TextField searchField;
    @FXML private ComboBox<String> categoryComboBox;
    @FXML private VBox faqResultsContainer;

    // Services
    private final KnowledgeAssistantService knowledgeService;
    private final AiAssistantService aiService;

    // State
    private File selectedImageFile;
    private byte[] selectedImageBytes;
    private ToggleGroup categoryToggleGroup;

    public KnowledgeAssistantController() {
        this(new KnowledgeAssistantServiceImpl(), new AiAssistantServiceImpl());
    }

    public KnowledgeAssistantController(KnowledgeAssistantService knowledgeService, AiAssistantService aiService) {
        this.knowledgeService = knowledgeService;
        this.aiService = aiService;
    }

    @FXML
    public void initialize() {
        setupAiStatusBadge();
        setupCategoryToggleGroup();
        setupInitialWelcomeChat();

        // Setup offline FAQ tab
        if (categoryComboBox != null) {
            categoryComboBox.setItems(FXCollections.observableArrayList(
                    "All Categories", "Milk Handling", "Paneer Care", "Boiling Technique", "Ghee Storage",
                    "Troubleshooting", "Curd Storage", "Dairy Hygiene", "Cow Health", "Cattle Feeding",
                    "Nutrition Comparison", "Customer Support"
            ));
            categoryComboBox.setValue("All Categories");
        }
        handleShowAll();
    }

    private void setupAiStatusBadge() {
        if (aiStatusBadge == null) return;
        if (aiService.isAiConfigured()) {
            aiStatusBadge.setText("🟢 " + aiService.getActiveProviderName());
            aiStatusBadge.getStyleClass().setAll("badge-ai");
        } else {
            aiStatusBadge.setText("⚠️ " + aiService.getActiveProviderName());
            aiStatusBadge.getStyleClass().setAll("badge-offline");
        }
    }

    private void setupCategoryToggleGroup() {
        categoryToggleGroup = new ToggleGroup();
        if (radioAnimalHealth != null) {
            radioAnimalHealth.setToggleGroup(categoryToggleGroup);
            radioAnimalHealth.setSelected(true);
        }
        if (radioMilkProblems != null) {
            radioMilkProblems.setToggleGroup(categoryToggleGroup);
        }
    }

    private void setupInitialWelcomeChat() {
        if (chatContainer == null) return;
        chatContainer.getChildren().clear();

        boolean configured = aiService.isAiConfigured();
        String welcomeText = "👋 Welcome to the **AgroDairy Smart Knowledge Assistant**!\n\n" +
                "I can assist with cattle welfare, milk boiling guidelines, paneer & curd preparation, hygiene, and storage questions.\n\n" +
                "📸 **Vision Image Analysis:** Upload a photo on the right panel to inspect:\n" +
                "• **Cow/buffalo health concerns** (observable lesions, swelling, posture, coat condition)\n" +
                "• **Milk-related problems** (curdling, discoloration, physical abnormalities)\n\n" +
                (configured
                        ? "🟢 Live AI is active and connected to " + aiService.getActiveProviderName() + "."
                        : "ℹ️ Live AI is currently in **Offline Fallback Mode**. Inquiries will use the local offline knowledge base. To activate live AI, click '⚙️ AI Setup Guide' above.");

        addAssistantMessage(welcomeText, "AgroDairy Assistant");
    }

    // =========================================================================
    // CHAT ACTIONS
    // =========================================================================

    @FXML
    public void handleSendMessage() {
        if (chatInputField == null) return;
        String query = chatInputField.getText() != null ? chatInputField.getText().trim() : "";
        if (query.isEmpty()) return;

        chatInputField.clear();
        addUserMessage(query);

        setChatLoading(true, "Consulting AI knowledge base...");

        if (aiService.isAiConfigured()) {
            aiService.askAiAsync(query)
                    .thenAccept(response -> Platform.runLater(() -> {
                        setChatLoading(false, null);
                        addAssistantMessage(response, aiService.getActiveProviderName());
                    }))
                    .exceptionally(ex -> {
                        logger.error("Error communicating with AI service", ex);
                        Platform.runLater(() -> {
                            setChatLoading(false, null);
                            addAssistantMessage("⚠️ Error: Unable to complete AI request: " + ex.getMessage(), "System Error");
                        });
                        return null;
                    });
        } else {
            // Graceful fallback to offline local FAQ knowledge base
            Platform.runLater(() -> {
                setChatLoading(false, null);
                try {
                    FAQKnowledgeBase bestFaq = knowledgeService.getBestMatchingFAQ(query);
                    String fallbackResponse = "📚 [Offline Knowledge Base Fallback]\n\n" +
                            "**Q: " + bestFaq.getQuestion() + "**\n\n" +
                            bestFaq.getAnswer() + "\n\n" +
                            "*(Note: Live AI API key is not configured. Displaying best match from local FAQ directory. Configure GEMINI_API_KEY for live responses.)*";
                    addAssistantMessage(fallbackResponse, "Offline Knowledge Base");
                } catch (Exception e) {
                    addAssistantMessage("No direct matching offline article found. Please browse the 'Offline FAQ Directory' tab or configure the AI API key.", "Offline Knowledge Base");
                }
            });
        }
    }

    @FXML
    public void handleClearChat() {
        setupInitialWelcomeChat();
    }

    // =========================================================================
    // VISION IMAGE ANALYSIS ACTIONS
    // =========================================================================

    @FXML
    public void handleUploadImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Animal or Milk Image for AI Inspection");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files (*.jpg, *.jpeg, *.png)", "*.jpg", "*.jpeg", "*.png", "*.JPG", "*.JPEG", "*.PNG")
        );

        File file = fileChooser.showOpenDialog(null);
        if (file == null) return;

        try {
            if (file.length() > 10 * 1024 * 1024) {
                AlertUtils.showError("Image Too Large", "Selected image exceeds the 10 MB limit. Please choose a smaller photo.");
                return;
            }

            byte[] bytes = Files.readAllBytes(file.toPath());
            Image fxImage = new Image(new ByteArrayInputStream(bytes));
            if (fxImage.isError()) {
                AlertUtils.showError("Invalid Image", "Unable to decode image file. Please select a valid JPG or PNG photo.");
                return;
            }

            selectedImageFile = file;
            selectedImageBytes = bytes;

            if (imagePreviewView != null) {
                imagePreviewView.setImage(fxImage);
                imagePreviewView.setVisible(true);
            }
            if (emptyPreviewPlaceholder != null) {
                emptyPreviewPlaceholder.setVisible(false);
            }
            if (removeImageBtn != null) removeImageBtn.setDisable(false);
            if (analyzeImageBtn != null) analyzeImageBtn.setDisable(false);
            if (imageDetailsLabel != null) {
                imageDetailsLabel.setText(String.format("%s (%.1f KB)", file.getName(), file.length() / 1024.0));
            }
        } catch (Exception e) {
            logger.error("Failed to load uploaded image", e);
            AlertUtils.showError("File Error", "Could not load selected image: " + e.getMessage());
        }
    }

    @FXML
    public void handleRemoveImage() {
        selectedImageFile = null;
        selectedImageBytes = null;

        if (imagePreviewView != null) {
            imagePreviewView.setImage(null);
            imagePreviewView.setVisible(false);
        }
        if (emptyPreviewPlaceholder != null) {
            emptyPreviewPlaceholder.setVisible(true);
        }
        if (removeImageBtn != null) removeImageBtn.setDisable(true);
        if (analyzeImageBtn != null) analyzeImageBtn.setDisable(true);
        if (imageDetailsLabel != null) {
            imageDetailsLabel.setText("Supported formats: JPG, JPEG, PNG (max 10 MB)");
        }
    }

    @FXML
    public void handleAnalyzeImage() {
        if (selectedImageBytes == null || selectedImageFile == null) {
            AlertUtils.showWarning("No Image", "Please upload an image before requesting analysis.");
            return;
        }

        ImageAnalysisCategory category = (radioAnimalHealth != null && radioAnimalHealth.isSelected())
                ? ImageAnalysisCategory.ANIMAL_HEALTH
                : ImageAnalysisCategory.MILK_PROBLEMS;

        String userNote = imageNotesField != null && imageNotesField.getText() != null
                ? imageNotesField.getText().trim() : "";

        String mimeType = selectedImageFile.getName().toLowerCase().endsWith(".png") ? "image/png" : "image/jpeg";

        // Display user message in chat with thumbnail
        addUserImageMessage(selectedImageBytes, category.getDisplayName(), userNote);

        // Disable actions during analysis
        if (analyzeImageBtn != null) analyzeImageBtn.setDisable(true);
        if (uploadImageBtn != null) uploadImageBtn.setDisable(true);
        setChatLoading(true, "Analyzing image features with genuine Vision AI...");

        aiService.analyzeImageAsync(selectedImageBytes, mimeType, category, userNote)
                .thenAccept(analysisResult -> Platform.runLater(() -> {
                    setChatLoading(false, null);
                    if (analyzeImageBtn != null) analyzeImageBtn.setDisable(false);
                    if (uploadImageBtn != null) uploadImageBtn.setDisable(false);

                    addVisionAnalysisMessage(analysisResult, category);
                }))
                .exceptionally(ex -> {
                    logger.error("Vision AI analysis failed", ex);
                    Platform.runLater(() -> {
                        setChatLoading(false, null);
                        if (analyzeImageBtn != null) analyzeImageBtn.setDisable(false);
                        if (uploadImageBtn != null) uploadImageBtn.setDisable(false);

                        addAssistantMessage("⚠️ Vision Analysis Error: " + ex.getMessage(), "System Error");
                    });
                    return null;
                });
    }

    @FXML
    public void handleShowApiHelp() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("AI Assistant & Vision Configuration");
        dialog.setHeaderText("Configure Google Gemini or OpenAI Vision API");

        ButtonType closeButtonType = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().add(closeButtonType);

        VBox contentBox = new VBox(14);
        contentBox.setPrefWidth(560);
        contentBox.setStyle("-fx-padding: 14;");

        // Status Card
        VBox statusCard = new VBox(4);
        statusCard.setStyle("-fx-background-color: " + (aiService.isAiConfigured() ? "#DEF7EC" : "#FFFBEB") + "; " +
                "-fx-border-color: " + (aiService.isAiConfigured() ? "#A7F3D0" : "#FDE68A") + "; " +
                "-fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 10;");
        Label statusHeading = new Label("Current Status:");
        statusHeading.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: " + (aiService.isAiConfigured() ? "#03543F" : "#92400E") + ";");
        Label statusValue = new Label(aiService.getActiveProviderName());
        statusValue.setStyle("-fx-font-size: 13.5px; -fx-font-weight: bold; -fx-text-fill: " + (aiService.isAiConfigured() ? "#065F46" : "#B45309") + ";");
        statusCard.getChildren().addAll(statusHeading, statusValue);

        // Direct In-App Activation Section
        VBox inputSection = new VBox(8);
        inputSection.setStyle("-fx-background-color: #F8FAFC; -fx-border-color: #E2E8F0; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 12;");
        
        Label inputTitle = new Label("🔑 Activate Live AI Directly (In-App)");
        inputTitle.setStyle("-fx-font-size: 13.5px; -fx-font-weight: bold; -fx-text-fill: #1E293B;");
        
        Label inputDesc = new Label("Get a free key at Google AI Studio (https://aistudio.google.com), paste it below, and click Activate:");
        inputDesc.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11.5px;");
        inputDesc.setWrapText(true);

        HBox providerBox = new HBox(10);
        providerBox.setAlignment(Pos.CENTER_LEFT);
        Label provLabel = new Label("Provider:");
        provLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
        ComboBox<String> providerCombo = new ComboBox<>(FXCollections.observableArrayList(
                "Google Gemini Vision (Free & Recommended)", "OpenAI Vision"
        ));
        providerCombo.setValue("Google Gemini Vision (Free & Recommended)");
        providerCombo.setPrefWidth(320);
        providerBox.getChildren().addAll(provLabel, providerCombo);

        HBox keyInputBox = new HBox(8);
        keyInputBox.setAlignment(Pos.CENTER_LEFT);
        PasswordField keyField = new PasswordField();
        keyField.setPromptText("Paste your API key here (e.g. AIzaSy... or sk-...)");
        keyField.setPrefHeight(36);
        HBox.setHgrow(keyField, Priority.ALWAYS);

        Button saveBtn = new Button("Save & Activate");
        saveBtn.getStyleClass().add("btn-success");
        saveBtn.setPrefHeight(36);

        Button clearBtn = new Button("Clear Key");
        clearBtn.getStyleClass().add("btn-danger");
        clearBtn.setPrefHeight(36);

        keyInputBox.getChildren().addAll(keyField, saveBtn, clearBtn);

        Label saveNotice = new Label();
        saveNotice.setWrapText(true);
        saveNotice.setStyle("-fx-font-size: 11.5px; -fx-font-weight: bold;");

        saveBtn.setOnAction(e -> {
            String enteredKey = keyField.getText() != null ? keyField.getText().trim() : "";
            if (enteredKey.isEmpty()) {
                saveNotice.setText("⚠️ Please paste a valid API key string.");
                saveNotice.setStyle("-fx-text-fill: #DC2626; -fx-font-size: 11.5px; -fx-font-weight: bold;");
                return;
            }
            String selectedProvider = providerCombo.getValue().contains("OpenAI") ? "OPENAI" : "GEMINI";
            boolean saved = aiService.persistApiKey(selectedProvider, enteredKey);
            setupAiStatusBadge();
            setupInitialWelcomeChat();
            if (saved) {
                saveNotice.setText("✅ API Key successfully saved and activated for " + aiService.getActiveProviderName() + "!");
                saveNotice.setStyle("-fx-text-fill: #059669; -fx-font-size: 11.5px; -fx-font-weight: bold;");
                statusHeading.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #03543F;");
                statusValue.setText(aiService.getActiveProviderName());
                statusValue.setStyle("-fx-font-size: 13.5px; -fx-font-weight: bold; -fx-text-fill: #065F46;");
                statusCard.setStyle("-fx-background-color: #DEF7EC; -fx-border-color: #A7F3D0; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 10;");
                keyField.clear();
            } else {
                saveNotice.setText("⚠️ Key activated in memory but could not be saved to disk.");
                saveNotice.setStyle("-fx-text-fill: #D97706; -fx-font-size: 11.5px; -fx-font-weight: bold;");
            }
        });

        clearBtn.setOnAction(e -> {
            aiService.clearApiKeys();
            aiService.persistApiKey("GEMINI", "");
            setupAiStatusBadge();
            setupInitialWelcomeChat();
            saveNotice.setText("ℹ️ Keys cleared. System reverted to Offline Fallback Mode.");
            saveNotice.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11.5px; -fx-font-weight: bold;");
            statusHeading.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #92400E;");
            statusValue.setText(aiService.getActiveProviderName());
            statusValue.setStyle("-fx-font-size: 13.5px; -fx-font-weight: bold; -fx-text-fill: #B45309;");
            statusCard.setStyle("-fx-background-color: #FFFBEB; -fx-border-color: #FDE68A; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 10;");
        });

        inputSection.getChildren().addAll(inputTitle, inputDesc, providerBox, keyInputBox, saveNotice);

        // Terminal Instructions Section
        VBox terminalSection = new VBox(6);
        Label termTitle = new Label("💻 Alternatively, Configure via Terminal Environment Variable:");
        termTitle.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #334155;");
        TextArea termArea = new TextArea(
                "# Windows PowerShell (Before launching JavaFX):\n" +
                "$env:GEMINI_API_KEY=\"your_api_key_here\"\n" +
                "mvn javafx:run\n\n" +
                "# Windows Command Prompt (CMD):\n" +
                "set GEMINI_API_KEY=your_api_key_here\n" +
                "mvn javafx:run\n\n" +
                "# Or pass as Maven JVM option:\n" +
                "mvn javafx:run -DGEMINI_API_KEY=\"your_api_key_here\""
        );
        termArea.setEditable(false);
        termArea.setPrefRowCount(6);
        termArea.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 11px;");
        terminalSection.getChildren().addAll(termTitle, termArea);

        contentBox.getChildren().addAll(statusCard, inputSection, terminalSection);
        dialog.getDialogPane().setContent(contentBox);
        dialog.showAndWait();
    }

    // =========================================================================
    // OFFLINE FAQ DIRECTORY ACTIONS (TAB 2)
    // =========================================================================

    @FXML
    public void handleSearch() {
        if (searchField == null) return;
        String query = searchField.getText() != null ? searchField.getText().trim() : "";
        String cat = categoryComboBox != null ? categoryComboBox.getValue() : "All Categories";

        if (query.isEmpty() && (cat == null || cat.equals("All Categories"))) {
            handleShowAll();
            return;
        }

        if (!query.isEmpty()) {
            try {
                FAQKnowledgeBase best = knowledgeService.getBestMatchingFAQ(query);
                displayFAQList(List.of(best));
            } catch (Exception e) {
                displayFAQList(List.of());
            }
        } else {
            List<FAQKnowledgeBase> list = knowledgeService.getFAQsByCategory(cat);
            displayFAQList(list);
        }
    }

    @FXML
    public void handleShowAll() {
        if (searchField != null) searchField.clear();
        if (categoryComboBox != null) categoryComboBox.setValue("All Categories");
        List<FAQKnowledgeBase> all = knowledgeService.getAllFAQs();
        displayFAQList(all);
    }

    private void displayFAQList(List<FAQKnowledgeBase> list) {
        if (faqResultsContainer == null) return;
        faqResultsContainer.getChildren().clear();

        if (list == null || list.isEmpty()) {
            Label empty = new Label("No matching local FAQ articles found.");
            empty.setStyle("-fx-text-fill: #64748B; -fx-font-size: 14px; -fx-padding: 20;");
            faqResultsContainer.getChildren().add(empty);
            return;
        }

        for (FAQKnowledgeBase faq : list) {
            VBox card = new VBox();
            card.getStyleClass().add("faq-card");

            Label qLabel = new Label("❓ " + faq.getQuestion());
            qLabel.getStyleClass().add("faq-question");

            Label aLabel = new Label(faq.getAnswer());
            aLabel.getStyleClass().add("faq-answer");
            aLabel.setWrapText(true);

            Label metaLabel = new Label(String.format("Category: %s | Tags: %s", faq.getCategory(), faq.getTags()));
            metaLabel.setStyle("-fx-text-fill: #2563EB; -fx-font-size: 12px; -fx-font-weight: bold;");

            card.getChildren().addAll(qLabel, aLabel, metaLabel);
            faqResultsContainer.getChildren().add(card);
        }
    }

    // =========================================================================
    // UI CHAT BUBBLE HELPERS
    // =========================================================================

    private void addUserMessage(String text) {
        if (chatContainer == null) return;

        HBox wrapper = new HBox();
        wrapper.setAlignment(Pos.CENTER_RIGHT);

        VBox bubble = new VBox(4);
        bubble.getStyleClass().add("chat-bubble-user");
        bubble.setMaxWidth(520);

        Label content = new Label(text);
        content.setWrapText(true);
        content.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 13.5px;");

        Label time = new Label(LocalTime.now().format(TIME_FMT));
        time.setStyle("-fx-text-fill: #BFDBFE; -fx-font-size: 10px; -fx-alignment: BASELINE_RIGHT;");

        bubble.getChildren().addAll(content, time);
        wrapper.getChildren().add(bubble);
        chatContainer.getChildren().add(wrapper);
        scrollToBottom();
    }

    private void addUserImageMessage(byte[] imageBytes, String categoryName, String userNote) {
        if (chatContainer == null) return;

        HBox wrapper = new HBox();
        wrapper.setAlignment(Pos.CENTER_RIGHT);

        VBox bubble = new VBox(6);
        bubble.getStyleClass().add("chat-bubble-user");
        bubble.setMaxWidth(520);

        Label header = new Label("📸 Image Uploaded for Analysis");
        header.setStyle("-fx-font-weight: bold; -fx-text-fill: #FFFFFF; -fx-font-size: 13.5px;");

        Label catLabel = new Label("Inspection Category: " + categoryName);
        catLabel.setStyle("-fx-text-fill: #E0E7FF; -fx-font-size: 12px;");

        ImageView thumbnail = new ImageView(new Image(new ByteArrayInputStream(imageBytes)));
        thumbnail.setFitWidth(180);
        thumbnail.setFitHeight(120);
        thumbnail.setPreserveRatio(true);
        thumbnail.setSmooth(true);
        thumbnail.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 4, 0, 0, 1);");

        bubble.getChildren().addAll(header, catLabel, thumbnail);

        if (userNote != null && !userNote.isEmpty()) {
            Label noteLabel = new Label("Note: " + userNote);
            noteLabel.setWrapText(true);
            noteLabel.setStyle("-fx-text-fill: #FFFFFF; -fx-font-style: italic; -fx-font-size: 12px;");
            bubble.getChildren().add(noteLabel);
        }

        Label time = new Label(LocalTime.now().format(TIME_FMT));
        time.setStyle("-fx-text-fill: #BFDBFE; -fx-font-size: 10px;");
        bubble.getChildren().add(time);

        wrapper.getChildren().add(bubble);
        chatContainer.getChildren().add(wrapper);
        scrollToBottom();
    }

    private void addAssistantMessage(String text, String sourceLabel) {
        if (chatContainer == null) return;

        HBox wrapper = new HBox();
        wrapper.setAlignment(Pos.CENTER_LEFT);

        VBox bubble = new VBox(6);
        bubble.getStyleClass().add("chat-bubble-assistant");
        bubble.setMaxWidth(580);

        HBox metaHeader = new HBox(8);
        metaHeader.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("🤖 " + sourceLabel);
        title.setStyle("-fx-font-weight: bold; -fx-text-fill: #2563EB; -fx-font-size: 12.5px;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label time = new Label(LocalTime.now().format(TIME_FMT));
        time.getStyleClass().add("chat-meta-label");
        metaHeader.getChildren().addAll(title, spacer, time);

        Label content = new Label(text);
        content.setWrapText(true);
        content.setStyle("-fx-text-fill: #1E293B; -fx-font-size: 13px; -fx-line-spacing: 2;");

        bubble.getChildren().addAll(metaHeader, content);
        wrapper.getChildren().add(bubble);
        chatContainer.getChildren().add(wrapper);
        scrollToBottom();
    }

    private void addVisionAnalysisMessage(String analysisText, ImageAnalysisCategory category) {
        if (chatContainer == null) return;

        HBox wrapper = new HBox();
        wrapper.setAlignment(Pos.CENTER_LEFT);

        VBox bubble = new VBox(10);
        bubble.getStyleClass().add("chat-bubble-vision");
        bubble.setMaxWidth(620);

        // Header
        HBox metaHeader = new HBox(8);
        metaHeader.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("🔬 AI Vision Analysis: " + category.getDisplayName());
        title.setStyle("-fx-font-weight: bold; -fx-text-fill: #1E40AF; -fx-font-size: 14px;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label time = new Label(LocalTime.now().format(TIME_FMT));
        time.getStyleClass().add("chat-meta-label");
        metaHeader.getChildren().addAll(title, spacer, time);

        // Analysis Content
        Label content = new Label(analysisText);
        content.setWrapText(true);
        content.setStyle("-fx-text-fill: #1E293B; -fx-font-size: 13px; -fx-line-spacing: 2;");

        // Mandatory Disclaimer Banner
        VBox disclaimerBox = new VBox(3);
        disclaimerBox.setStyle("-fx-background-color: #FEF2F2; -fx-border-color: #FCA5A5; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 8;");
        Label disclTitle = new Label("⚠️ MANDATORY DISCLAIMER:");
        disclTitle.setStyle("-fx-text-fill: #991B1B; -fx-font-weight: bold; -fx-font-size: 11px;");

        String disclaimerText = (category == ImageAnalysisCategory.ANIMAL_HEALTH)
                ? "This observational report is generated by AI from visual features and DOES NOT constitute a veterinary clinical diagnosis. Always consult a licensed veterinarian for definitive diagnosis and treatment."
                : "Visual assessment CANNOT certify that milk is microbiologically safe or free of adulterants. Acidity, bacterial count, and pathogens require formal laboratory testing. Do not consume spoiled milk.";

        Label disclContent = new Label(disclaimerText);
        disclContent.setWrapText(true);
        disclContent.setStyle("-fx-text-fill: #7F1D1D; -fx-font-size: 10.5px;");
        disclaimerBox.getChildren().addAll(disclTitle, disclContent);

        bubble.getChildren().addAll(metaHeader, content, disclaimerBox);
        wrapper.getChildren().add(bubble);
        chatContainer.getChildren().add(wrapper);
        scrollToBottom();
    }

    private void setChatLoading(boolean loading, String message) {
        if (chatLoadingBox != null) {
            chatLoadingBox.setVisible(loading);
            chatLoadingBox.setManaged(loading);
        }
        if (chatLoadingLabel != null && message != null) {
            chatLoadingLabel.setText(message);
        }
        if (sendBtn != null) {
            sendBtn.setDisable(loading);
        }
    }

    private void scrollToBottom() {
        Platform.runLater(() -> {
            if (chatScrollPane != null) {
                chatScrollPane.layout();
                chatScrollPane.setVvalue(1.0);
            }
        });
    }
}
