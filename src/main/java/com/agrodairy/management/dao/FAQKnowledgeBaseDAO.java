package com.agrodairy.management.dao;

import com.agrodairy.management.model.FAQKnowledgeBase;
import java.util.List;
import java.util.Optional;

public interface FAQKnowledgeBaseDAO {
    Long insert(FAQKnowledgeBase faq);
    boolean update(FAQKnowledgeBase faq);
    boolean delete(Long faqId);
    Optional<FAQKnowledgeBase> findById(Long faqId);
    List<FAQKnowledgeBase> findAll();
    List<FAQKnowledgeBase> findByCategory(String category);
    List<FAQKnowledgeBase> searchByKeyword(String keyword);
}
