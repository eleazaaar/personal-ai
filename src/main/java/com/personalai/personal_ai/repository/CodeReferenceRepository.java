package com.personalai.personal_ai.repository;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CodeReferenceRepository {

    private final JdbcTemplate jdbcTemplate;

    public CodeReferenceRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS code_references (
                    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                    conversation_id VARCHAR(100) NOT NULL,
                    filename VARCHAR(255) NOT NULL,
                    content LONGTEXT NOT NULL,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (id),
                    KEY idx_code_reference_conversation (conversation_id, id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
    }

    public void save(String conversationId, String filename, String content) {
        jdbcTemplate.update("""
                INSERT INTO code_references (conversation_id, filename, content)
                VALUES (?, ?, ?)
                """, conversationId, filename, content);
    }

    public List<CodeReference> findByConversationId(String conversationId) {
        return jdbcTemplate.query("""
                SELECT filename, content FROM code_references
                WHERE conversation_id = ?
                ORDER BY id ASC
                """, (rs, rowNum) -> new CodeReference(
                rs.getString("filename"), rs.getString("content")), conversationId);
    }

    public List<CodeReference> findAll() {
        return jdbcTemplate.query("""
                SELECT filename, content FROM code_references
                ORDER BY id ASC
                """, (rs, rowNum) -> new CodeReference(
                rs.getString("filename"), rs.getString("content")));
    }

    public record CodeReference(String filename, String content) {}
}
