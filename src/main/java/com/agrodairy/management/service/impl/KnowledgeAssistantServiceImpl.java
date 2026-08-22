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
        List<FAQKnowledgeBase> all = getAllFAQs();
        if (searchQuery == null || searchQuery.trim().isEmpty()) {
            return all;
        }
        String q = searchQuery.trim().toLowerCase();
        return all.stream()
                .filter(faq -> (faq.getQuestion() != null && faq.getQuestion().toLowerCase().contains(q)) ||
                               (faq.getKeywords() != null && faq.getKeywords().toLowerCase().contains(q)) ||
                               (faq.getCategory() != null && faq.getCategory().toLowerCase().contains(q)) ||
                               (faq.getAnswer() != null && faq.getAnswer().toLowerCase().contains(q)))
                .collect(Collectors.toList());
    }

    @Override
    public FAQKnowledgeBase getBestMatchingFAQ(String userQuery) {
        if (userQuery == null || userQuery.trim().isEmpty()) {
            throw new ValidationException("Question query cannot be empty.");
        }

        List<FAQKnowledgeBase> allFaqs = getAllFAQs();
        if (allFaqs.isEmpty()) {
            return createFallbackFAQ("No FAQ articles available in local knowledge base.");
        }

        String[] tokens = userQuery.toLowerCase().replaceAll("[^a-zA-Z0-9\\s]", "").split("\\s+");
        Set<String> queryTokens = Arrays.stream(tokens)
                .filter(t -> t.length() > 2) // Filter out short stop words
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
        if (category == null || category.trim().isEmpty() || category.equalsIgnoreCase("All Categories")) {
            return getAllFAQs();
        }
        return getAllFAQs().stream()
                .filter(f -> f.getCategory() != null && f.getCategory().equalsIgnoreCase(category.trim()))
                .collect(Collectors.toList());
    }

    @Override
    public List<FAQKnowledgeBase> getAllFAQs() {
        try {
            List<FAQKnowledgeBase> list = faqKnowledgeBaseDAO.findAll();
            if (list != null && !list.isEmpty()) {
                return list;
            }
        } catch (Exception e) {
            // Database offline or failed, fall back to offline JSON resource
        }
        return loadFromJsonResource();
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

    private List<FAQKnowledgeBase> loadFromJsonResource() {
        List<FAQKnowledgeBase> list = new ArrayList<>();
        try (java.io.InputStream is = getClass().getResourceAsStream("/json/dairy_knowledge_base.json")) {
            if (is == null) return list;
            String jsonText = new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            // Simple regex parser for FAQ objects from JSON
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\{\\s*\"faq_id\":\\s*(\\d+),\\s*\"question\":\\s*\"([^\"]+)\",\\s*\"answer\":\\s*\"([^\"]+)\",\\s*\"category\":\\s*\"([^\"]+)\",\\s*\"keywords\":\\s*\"([^\"]+)\",\\s*\"tags\":\\s*\"([^\"]+)\"\\s*\\}");
            java.util.regex.Matcher matcher = pattern.matcher(jsonText);
            while (matcher.find()) {
                FAQKnowledgeBase faq = new FAQKnowledgeBase();
                faq.setFaqId(Long.parseLong(matcher.group(1)));
                faq.setQuestion(matcher.group(2));
                faq.setAnswer(matcher.group(3));
                faq.setCategory(matcher.group(4));
                faq.setKeywords(matcher.group(5));
                faq.setTags(matcher.group(6));
                list.add(faq);
            }
        } catch (Exception ignored) {}
        return list;
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
