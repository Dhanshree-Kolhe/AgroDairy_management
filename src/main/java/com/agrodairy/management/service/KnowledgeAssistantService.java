package com.agrodairy.management.service;

import com.agrodairy.management.model.FAQKnowledgeBase;

import java.util.List;

public interface KnowledgeAssistantService {
    List<FAQKnowledgeBase> searchFAQs(String searchQuery);
    FAQKnowledgeBase getBestMatchingFAQ(String userQuery);
    List<FAQKnowledgeBase> getFAQsByCategory(String category);
    List<FAQKnowledgeBase> getAllFAQs();
    FAQKnowledgeBase addFAQ(FAQKnowledgeBase faq);
}
