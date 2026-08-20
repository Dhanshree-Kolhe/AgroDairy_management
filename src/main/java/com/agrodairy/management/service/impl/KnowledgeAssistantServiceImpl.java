package com.agrodairy.management.service.impl;

import com.agrodairy.management.dao.FAQKnowledgeBaseDAO;
import com.agrodairy.management.dao.impl.FAQKnowledgeBaseDAOImpl;
import com.agrodairy.management.exception.ValidationException;
import com.agrodairy.management.model.FAQKnowledgeBase;
import com.agrodairy.management.service.KnowledgeAssistantService;

import java.util.*;
import java.util.stream.Collectors;

public class KnowledgeAssistantServiceImpl implements KnowledgeAssistantService {
    private final FAQKnowledgeBaseDAO faqKnowledgeBaseDAO;

    public KnowledgeAssistantServiceImpl() {
        this.faqKnowledgeBaseDAO = new FAQKnowledgeBaseDAOImpl();
    }

    public KnowledgeAssistantServiceImpl(FAQKnowledgeBaseDAO faqKnowledgeBaseDAO) {
        this.faqKnowledgeBaseDAO = faqKnowledgeBaseDAO;
    }

    @Override
    public List<FAQKnowledgeBase> searchFAQs(String searchQuery) {
        if (searchQuery == null || searchQuery.trim().isEmpty()) {
            return faqKnowledgeBaseDAO.findAll();
        }
        return faqKnowledgeBaseDAO.searchByKeyword(searchQuery.trim());
    }

    @Override
    public FAQKnowledgeBase getBestMatchingFAQ(String userQuery) {
        if (userQuery == null || userQuery.trim().isEmpty()) {
            throw new ValidationException("Question query cannot be empty.");
        }

        List<FAQKnowledgeBase> allFaqs = faqKnowledgeBaseDAO.findAll();
        if (allFaqs.isEmpty()) {
            return createFallbackFAQ("No FAQ articles available in local knowledge base.");
        }

        String[] tokens = userQuery.toLowerCase().replaceAll("[^a-zA-Z0-9\\s]", "").split("\\s+");
        Set<String> queryTokens = Arrays.stream(tokens)
                .filter(t -> t.length() > 2) // Filter out short stop words (is, in, to, a, or, etc.)
                .collect(Collectors.toSet());

        if (queryTokens.isEmpty()) {
            return allFaqs.get(0);
        }

        FAQKnowledgeBase bestMatch = null;
        int maxScore = 0;

        for (FAQKnowledgeBase faq : allFaqs) {
            int score = 0;
            String question = faq.getQuestion() != null ? faq.getQuestion().toLowerCase() : "";
            String keywords = faq.getKeywords() != null ? faq.getKeywords().toLowerCase() : "";
            String tags = faq.getTags() != null ? faq.getTags().toLowerCase() : "";
            String answer = faq.getAnswer() != null ? faq.getAnswer().toLowerCase() : "";

            for (String token : queryTokens) {
                if (keywords.contains(token)) score += 5;
                if (question.contains(token)) score += 3;
                if (tags.contains(token)) score += 2;
                if (answer.contains(token)) score += 1;
            }

            if (score > maxScore) {
                maxScore = score;
                bestMatch = faq;
            }
        }

        if (bestMatch != null && maxScore > 0) {
            return bestMatch;
        }

        return createFallbackFAQ("Sorry, no direct match was found in the offline Dairy Knowledge Assistant for your query: '" + userQuery + "'. Please browse categories for common milk storage and cattle feeding guidelines.");
    }

    @Override
    public List<FAQKnowledgeBase> getFAQsByCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            return faqKnowledgeBaseDAO.findAll();
        }
        return faqKnowledgeBaseDAO.findByCategory(category.trim());
    }

    @Override
    public List<FAQKnowledgeBase> getAllFAQs() {
        return faqKnowledgeBaseDAO.findAll();
    }

    @Override
    public FAQKnowledgeBase addFAQ(FAQKnowledgeBase faq) {
        if (faq == null) throw new ValidationException("FAQ object cannot be null.");
        if (faq.getQuestion() == null || faq.getQuestion().trim().isEmpty()) {
            throw new ValidationException("Question is required.");
        }
        if (faq.getAnswer() == null || faq.getAnswer().trim().isEmpty()) {
            throw new ValidationException("Answer is required.");
        }

        Long id = faqKnowledgeBaseDAO.insert(faq);
        faq.setFaqId(id);
        return faq;
    }

    private FAQKnowledgeBase createFallbackFAQ(String fallbackMessage) {
        FAQKnowledgeBase fallback = new FAQKnowledgeBase();
        fallback.setFaqId(-1L);
        fallback.setQuestion("Offline Dairy Knowledge Assistance");
        fallback.setAnswer(fallbackMessage);
        fallback.setCategory("General Help");
        fallback.setKeywords("help, search, FAQ");
        fallback.setTags("Help");
        return fallback;
    }
}
