package com.agrodairy.management.service;

import com.agrodairy.management.dao.FAQKnowledgeBaseDAO;
import com.agrodairy.management.model.FAQKnowledgeBase;
import com.agrodairy.management.service.impl.KnowledgeAssistantServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class HolidayAndKnowledgeServiceTest {

    private KnowledgeAssistantService knowledgeService;
    private StubFAQKnowledgeBaseDAO faqDAO;

    @BeforeEach
    public void setUp() {
        faqDAO = new StubFAQKnowledgeBaseDAO();
        knowledgeService = new KnowledgeAssistantServiceImpl(faqDAO);

        FAQKnowledgeBase faq1 = new FAQKnowledgeBase();
        faq1.setFaqId(1L);
        faq1.setQuestion("How long can paneer stay in fridge?");
        faq1.setAnswer("Unopened fresh paneer lasts 3 to 5 days in a refrigerator.");
        faq1.setCategory("Paneer Care");
        faq1.setKeywords("paneer, fridge, store, shelf life");
        faq1.setTags("Paneer, Storage");

        FAQKnowledgeBase faq2 = new FAQKnowledgeBase();
        faq2.setFaqId(2L);
        faq2.setQuestion("How to boil cow milk properly?");
        faq2.setAnswer("Boil raw milk on medium heat within 2 hours of delivery.");
        faq2.setCategory("Milk Care");
        faq2.setKeywords("boil, cow milk, raw milk, temperature");
        faq2.setTags("Milk, Hygiene");

        faqDAO.insert(faq1);
        faqDAO.insert(faq2);
    }

    @Test
    public void test14_OfflineFAQKeywordSearch() {
        FAQKnowledgeBase match1 = knowledgeService.getBestMatchingFAQ("How long can paneer stay in fridge?");
        assertNotNull(match1);
        assertEquals(1L, match1.getFaqId());
        assertTrue(match1.getAnswer().contains("3 to 5 days"));

        FAQKnowledgeBase match2 = knowledgeService.getBestMatchingFAQ("boiling cow milk");
        assertNotNull(match2);
        assertEquals(2L, match2.getFaqId());
    }

    private static class StubFAQKnowledgeBaseDAO implements FAQKnowledgeBaseDAO {
        private final List<FAQKnowledgeBase> list = new ArrayList<>();
        private long seq = 1;

        @Override public Long insert(FAQKnowledgeBase faq) { faq.setFaqId(seq++); list.add(faq); return faq.getFaqId(); }
        @Override public boolean update(FAQKnowledgeBase faq) { return true; }
        @Override public boolean delete(Long id) { return true; }
        @Override public Optional<FAQKnowledgeBase> findById(Long id) { return list.stream().filter(f -> f.getFaqId().equals(id)).findFirst(); }
        @Override public List<FAQKnowledgeBase> findAll() { return list; }
        @Override public List<FAQKnowledgeBase> findByCategory(String c) { return list; }
        @Override public List<FAQKnowledgeBase> searchByKeyword(String kw) {
            List<FAQKnowledgeBase> res = new ArrayList<>();
            for (FAQKnowledgeBase f : list) {
                if (f.getQuestion().toLowerCase().contains(kw.toLowerCase()) || f.getKeywords().toLowerCase().contains(kw.toLowerCase())) {
                    res.add(f);
                }
            }
            return res;
        }
    }
}
