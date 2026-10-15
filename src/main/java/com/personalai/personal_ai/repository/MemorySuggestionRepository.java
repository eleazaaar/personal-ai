package com.personalai.personal_ai.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.sql.Statement;

@Repository
public class MemorySuggestionRepository {

    private final JdbcTemplate jdbcTemplate;

    public MemorySuggestionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS memory_suggestions (
                    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                    memory_text TEXT NOT NULL,
                    memory_type ENUM('fact','preference','episodic','inference','other') NOT NULL DEFAULT 'fact',
                    source_conversation_id VARCHAR(100) DEFAULT NULL,
                    source_message TEXT DEFAULT NULL,
                    status ENUM('pending','approved','rejected') NOT NULL DEFAULT 'pending',
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    reviewed_at TIMESTAMP NULL DEFAULT NULL,
                    PRIMARY KEY (id),
                    KEY idx_memory_status (status, created_at)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
    }

    public List<MemorySuggestion> findPending() {
        return jdbcTemplate.query(
                """
                SELECT id, memory_text, memory_type,
                       source_conversation_id, source_message,
                       status, created_at
                FROM memory_suggestions
                WHERE status = 'pending'
                ORDER BY created_at DESC, id DESC
                """,
                (rs, rowNum) -> new MemorySuggestion(
                        rs.getLong("id"),
                        rs.getString("memory_text"),
                        rs.getString("memory_type"),
                        rs.getString("source_conversation_id"),
                        rs.getString("source_message"),
                        rs.getString("status"),
                        rs.getTimestamp("created_at").toString()
                )
        );
    }

    public Optional<MemorySuggestion> findById(long id) {
        List<MemorySuggestion> results = jdbcTemplate.query(
                """
                SELECT id, memory_text, memory_type,
                       source_conversation_id, source_message,
                       status, created_at
                FROM memory_suggestions
                WHERE id = ?
                """,
                (rs, rowNum) -> new MemorySuggestion(
                        rs.getLong("id"),
                        rs.getString("memory_text"),
                        rs.getString("memory_type"),
                        rs.getString("source_conversation_id"),
                        rs.getString("source_message"),
                        rs.getString("status"),
                        rs.getTimestamp("created_at").toString()
                ),
                id
        );

        return results.stream().findFirst();
    }

    public List<String> findApprovedMemories() {
        return jdbcTemplate.query(
                """
                SELECT memory_text
                FROM personal_memories
                ORDER BY id ASC
                """,
                (rs, rowNum) -> rs.getString("memory_text")
        );
    }

    public void saveApprovedMemory(MemorySuggestion suggestion) {
        jdbcTemplate.update(
                """
                INSERT INTO personal_memories
                    (memory_text, memory_type, source_suggestion_id)
                VALUES (?, ?, ?)
                """,
                suggestion.memoryText(),
                suggestion.memoryType(),
                suggestion.id()
        );
    }

    public int updateStatus(long id, String status) {
        return jdbcTemplate.update(
                """
                UPDATE memory_suggestions
                SET status = ?, reviewed_at = CURRENT_TIMESTAMP
                WHERE id = ? AND status = 'pending'
                """,
                status,
                id
        );
    }
    
    public Long savePendingSuggestion(
            String memoryText,
            String memoryType,
            String conversationId,
            String sourceMessage) {

        if (memoryText == null || memoryText.isBlank()
                || memoryText.trim().length() > 1000) {
            return null;
        }

        String normalizedText = memoryText.trim();

        // Avoid duplicates among pending suggestions and approved memories.
        Integer existing = jdbcTemplate.queryForObject(
                """
                SELECT
                    (SELECT COUNT(*)
                    FROM memory_suggestions
                    WHERE status = 'pending'
                    AND LOWER(TRIM(memory_text)) = LOWER(TRIM(?)))
                    +
                    (SELECT COUNT(*)
                    FROM personal_memories
                    WHERE LOWER(TRIM(memory_text)) = LOWER(TRIM(?)))
                """,
                Integer.class,
                normalizedText,
                normalizedText
        );

        if (existing != null && existing > 0) {
            return null;
        }

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement(
                    """
                    INSERT INTO memory_suggestions (
                        memory_text,
                        memory_type,
                        source_conversation_id,
                        source_message,
                        status
                    )
                    VALUES (?, ?, ?, ?, 'pending')
                    """,
                    Statement.RETURN_GENERATED_KEYS
            );

            statement.setString(1, normalizedText);
            statement.setString(2, memoryType);
            statement.setString(3, conversationId);
            statement.setString(4, sourceMessage);

            return statement;
        }, keyHolder);

        Number key = keyHolder.getKey();
        return key == null ? null : key.longValue();
    }

    public record MemorySuggestion(
            long id,
            String memoryText,
            String memoryType,
            String sourceConversationId,
            String sourceMessage,
            String status,
            String createdAt
    ) {
    }
}