
package com.personalai.personal_ai.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ChatMessageRepository {

    private final JdbcTemplate jdbcTemplate;

    public ChatMessageRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
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
