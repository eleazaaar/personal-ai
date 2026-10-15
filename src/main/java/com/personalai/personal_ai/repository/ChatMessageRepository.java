
package com.personalai.personal_ai.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ChatMessageRepository {

    private final JdbcTemplate jdbcTemplate;

    public ChatMessageRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS chat_messages (
                    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                    conversation_id VARCHAR(100) NOT NULL,
                    role ENUM('user','assistant') NOT NULL,
                    content TEXT NOT NULL,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (id),
                    KEY idx_conversation (conversation_id, id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
    }

    public void save(String conversationId, String role, String content) {
        jdbcTemplate.update(
                """
                INSERT INTO chat_messages
                    (conversation_id, role, content)
                VALUES (?, ?, ?)
                """,
                conversationId,
                role,
                content
        );
    }

    public List<ChatMessage> findByConversationId(String conversationId) {
        return jdbcTemplate.query(
                """
                SELECT role, content
                FROM chat_messages
                WHERE conversation_id = ?
                ORDER BY id ASC
                """,
                (rs, rowNum) -> new ChatMessage(
                        rs.getString("role"),
                        rs.getString("content")
                ),
                conversationId
        );
    }

    public record ChatMessage(String role, String content) {
    }
}
