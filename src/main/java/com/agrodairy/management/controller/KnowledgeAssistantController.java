package com.agrodairy.management.controller;

import com.agrodairy.management.model.FAQKnowledgeBase;
import com.agrodairy.management.service.KnowledgeAssistantService;
import com.agrodairy.management.service.impl.KnowledgeAssistantServiceImpl;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.util.List;

public class KnowledgeAssistantController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> categoryComboBox;
    @FXML private VBox faqResultsContainer;

    private final KnowledgeAssistantService knowledgeService;

    public KnowledgeAssistantController() {
        this.knowledgeService = new KnowledgeAssistantServiceImpl();
    }

    @FXML
    public void initialize() {
        if (categoryComboBox != null) {
            categoryComboBox.setItems(FXCollections.observableArrayList(
                    "All Categories", "Milk Handling", "Paneer Care", "Boiling Technique", "Ghee Storage", "Troubleshooting", "Curd Making", "Nutrition Comparison", "Customer Support"
            ));
            categoryComboBox.setValue("All Categories");
        }

        handleShowAll();
    }

    @FXML
    public void handleSearch() {
        String query = searchField.getText() != null ? searchField.getText().trim() : "";
        String cat = categoryComboBox.getValue();

        if (query.isEmpty() && (cat == null || cat.equals("All Categories"))) {
            handleShowAll();
            return;
        }

        if (!query.isEmpty()) {
            FAQKnowledgeBase best = knowledgeService.getBestMatchingFAQ(query);
            displayFAQList(List.of(best));
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
}
