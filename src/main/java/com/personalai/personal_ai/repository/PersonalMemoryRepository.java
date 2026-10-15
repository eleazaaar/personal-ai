
package com.personalai.personal_ai.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class PersonalMemoryRepository {

    private final JdbcTemplate jdbcTemplate;

    public PersonalMemoryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<PersonalMemory> findAll() {
        return jdbcTemplate.query("""
                SELECT id, memory_text, memory_type,
                       source_suggestion_id, created_at, updated_at
                FROM personal_memories
                ORDER BY updated_at DESC, id DESC
                """,
                (rs, rowNum) -> new PersonalMemory(
                        rs.getLong("id"),
                        rs.getString("memory_text"),
                        rs.getString("memory_type"),
                        rs.getObject("source_suggestion_id", Long.class),
                        rs.getTimestamp("created_at"),
                        rs.getTimestamp("updated_at")
                ));
    }

    public Optional<PersonalMemory> findById(long id) {
        return jdbcTemplate.query("""
                SELECT id, memory_text, memory_type,
                       source_suggestion_id, created_at, updated_at
                FROM personal_memories
                WHERE id = ?
                """,
                (rs, rowNum) -> new PersonalMemory(
                        rs.getLong("id"),
                        rs.getString("memory_text"),
                        rs.getString("memory_type"),
                        rs.getObject("source_suggestion_id", Long.class),
                        rs.getTimestamp("created_at"),
                        rs.getTimestamp("updated_at")
                ), id)
                .stream()
                .findFirst();
    }

    public int create(String memoryText, String memoryType) {
        return jdbcTemplate.update("""
                INSERT INTO personal_memories
                    (memory_text, memory_type)
                VALUES (?, ?)
                """,
                memoryText, memoryType);
    }

    public int update(long id, String memoryText, String memoryType) {
        return jdbcTemplate.update("""
                UPDATE personal_memories
                SET memory_text = ?,
                    memory_type = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """,
                memoryText, memoryType, id);
    }

    public int delete(long id) {
        return jdbcTemplate.update("""
                DELETE FROM personal_memories
                WHERE id = ?
                """, id);
    }

    public record PersonalMemory(
            long id,
            String memoryText,
            String memoryType,
            Long sourceSuggestionId,
            java.sql.Timestamp createdAt,
            java.sql.Timestamp updatedAt
    ) {}
}