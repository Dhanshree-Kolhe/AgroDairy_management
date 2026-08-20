package com.agrodairy.management.dao.impl;

import com.agrodairy.management.dao.FAQKnowledgeBaseDAO;
import com.agrodairy.management.exception.DatabaseException;
import com.agrodairy.management.model.FAQKnowledgeBase;
import com.agrodairy.management.util.DatabaseUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FAQKnowledgeBaseDAOImpl implements FAQKnowledgeBaseDAO {

    @Override
    public Long insert(FAQKnowledgeBase faq) {
        String sql = "INSERT INTO faq_knowledge_base (question, answer, category, keywords, tags) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, faq.getQuestion());
            stmt.setString(2, faq.getAnswer());
            stmt.setString(3, faq.getCategory());
            stmt.setString(4, faq.getKeywords());
            stmt.setString(5, faq.getTags());

            int affected = stmt.executeUpdate();
            if (affected == 0) throw new DatabaseException("Creating FAQ entry failed, no rows affected.");

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    long id = generatedKeys.getLong(1);
                    faq.setFaqId(id);
                    return id;
                } else {
                    throw new DatabaseException("Creating FAQ entry failed, no ID obtained.");
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to insert FAQ entry: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean update(FAQKnowledgeBase faq) {
        String sql = "UPDATE faq_knowledge_base SET question = ?, answer = ?, category = ?, keywords = ?, tags = ? WHERE faq_id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, faq.getQuestion());
            stmt.setString(2, faq.getAnswer());
            stmt.setString(3, faq.getCategory());
            stmt.setString(4, faq.getKeywords());
            stmt.setString(5, faq.getTags());
            stmt.setLong(6, faq.getFaqId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update FAQ entry: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean delete(Long faqId) {
        String sql = "DELETE FROM faq_knowledge_base WHERE faq_id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, faqId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete FAQ entry: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<FAQKnowledgeBase> findById(Long faqId) {
        String sql = "SELECT * FROM faq_knowledge_base WHERE faq_id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, faqId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapResultSetToFAQ(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find FAQ by ID: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public List<FAQKnowledgeBase> findAll() {
        List<FAQKnowledgeBase> list = new ArrayList<>();
        String sql = "SELECT * FROM faq_knowledge_base ORDER BY faq_id ASC";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToFAQ(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to retrieve all FAQ entries: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public List<FAQKnowledgeBase> findByCategory(String category) {
        List<FAQKnowledgeBase> list = new ArrayList<>();
        String sql = "SELECT * FROM faq_knowledge_base WHERE category = ? ORDER BY faq_id ASC";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, category);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToFAQ(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find FAQ entries by category: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public List<FAQKnowledgeBase> searchByKeyword(String keyword) {
        List<FAQKnowledgeBase> list = new ArrayList<>();
        String pattern = "%" + (keyword != null ? keyword.trim().toLowerCase() : "") + "%";
        String sql = "SELECT * FROM faq_knowledge_base WHERE LOWER(question) LIKE ? OR LOWER(keywords) LIKE ? OR LOWER(tags) LIKE ? OR LOWER(answer) LIKE ? ORDER BY faq_id ASC";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, pattern);
            stmt.setString(2, pattern);
            stmt.setString(3, pattern);
            stmt.setString(4, pattern);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToFAQ(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to search FAQ entries by keyword: " + e.getMessage(), e);
        }
        return list;
    }

    private FAQKnowledgeBase mapResultSetToFAQ(ResultSet rs) throws SQLException {
        FAQKnowledgeBase faq = new FAQKnowledgeBase();
        faq.setFaqId(rs.getLong("faq_id"));
        faq.setQuestion(rs.getString("question"));
        faq.setAnswer(rs.getString("answer"));
        faq.setCategory(rs.getString("category"));
        faq.setKeywords(rs.getString("keywords"));
        faq.setTags(rs.getString("tags"));
        faq.setLastUpdated(rs.getTimestamp("last_updated"));
        return faq;
    }
}
