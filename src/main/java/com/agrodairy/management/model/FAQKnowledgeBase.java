package com.agrodairy.management.model;

import java.sql.Timestamp;
import java.util.Objects;

/**
 * Domain entity representing Offline Dairy Knowledge Assistant FAQ repository.
 */
public class FAQKnowledgeBase {
    private Long faqId;
    private String question;
    private String answer;
    private String category;
    private String keywords;
    private String tags;
    private Timestamp lastUpdated;

    public FAQKnowledgeBase() {}

    public FAQKnowledgeBase(Long faqId, String question, String answer, String category, String keywords, String tags) {
        this.faqId = faqId;
        this.question = question;
        this.answer = answer;
        this.category = category;
        this.keywords = keywords;
        this.tags = tags;
    }

    public Long getFaqId() { return faqId; }
    public void setFaqId(Long faqId) { this.faqId = faqId; }

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }

    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getKeywords() { return keywords; }
    public void setKeywords(String keywords) { this.keywords = keywords; }

    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }

    public Timestamp getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(Timestamp lastUpdated) { this.lastUpdated = lastUpdated; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FAQKnowledgeBase that = (FAQKnowledgeBase) o;
        return Objects.equals(faqId, that.faqId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(faqId);
    }

    @Override
    public String toString() {
        return "FAQKnowledgeBase{" +
                "faqId=" + faqId +
                ", question='" + question + '\'' +
                ", category='" + category + '\'' +
                ", keywords='" + keywords + '\'' +
                '}';
    }
}
