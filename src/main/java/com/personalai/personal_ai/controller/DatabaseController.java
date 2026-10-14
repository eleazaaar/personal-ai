package com.personalai.personal_ai.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class DatabaseController {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/api/database/status")
    public Map<String, String> databaseStatus() {
        try {
            String database = jdbcTemplate.queryForObject(
                    "SELECT DATABASE()",
                    String.class
            );

            return Map.of(
                    "status", "connected",
                    "database", database
            );
        } catch (Exception e) {
            return Map.of(
                    "status", "error",
                    "message", "Unable to connect to the database."
            );
        }
    }
}