package com.personalai.personal_ai.controller;

import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/account")
public class AccountController {

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    public AccountController(
            JdbcTemplate jdbcTemplate,
            PasswordEncoder passwordEncoder) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/create")
    public ResponseEntity<Map<String, String>> createAccount(
            @RequestBody CreateAccountRequest request,
            Authentication authentication) {

        if (request.username() == null
                || request.email() == null
                || request.password() == null) {
            return response(HttpStatus.BAD_REQUEST,
                    "Username, email, and password are required.");
        }

        String username = request.username().trim();
        String email = request.email().trim();
        String password = request.password();

        if (!username.matches("[A-Za-z0-9_.-]{3,100}")) {
            return response(HttpStatus.BAD_REQUEST,
                    "Username must be 3–100 characters and contain only letters, numbers, dots, underscores, or hyphens.");
        }

        if (email.length() > 255
                || !Pattern.matches(
                    "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$", email)) {
            return response(HttpStatus.BAD_REQUEST,
                    "Enter a valid email address.");
        }

        if (password.length() < 12 || password.length() > 72) {
            return response(HttpStatus.BAD_REQUEST,
                    "Password must be between 12 and 72 characters.");
        }

        Integer existing = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE username = ? OR email = ?",
                Integer.class, username, email);

        if (existing != null && existing > 0) {
            return response(HttpStatus.CONFLICT,
                    "That username or email is already registered.");
        }

        jdbcTemplate.update(
                """
                INSERT INTO users
                    (username, email, password_hash, role, enabled)
                VALUES (?, ?, ?, 'OWNER', 1)
                """,
                username,
                email,
                passwordEncoder.encode(password));

        return response(HttpStatus.CREATED,
                "Owner account created successfully.");
    }

    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            @RequestBody ChangePasswordRequest request,
            Authentication authentication) {

        if (request.currentPassword() == null
                || request.newPassword() == null) {
            return response(HttpStatus.BAD_REQUEST,
                    "Current and new passwords are required.");
        }

        String username = authentication.getName();

        String storedHash = jdbcTemplate.query(
                "SELECT password_hash FROM users WHERE username = ? AND enabled = 1",
                (rs, rowNum) -> rs.getString("password_hash"),
                username
        ).stream().findFirst().orElse(null);

        if (storedHash == null
                || !passwordEncoder.matches(
                    request.currentPassword(), storedHash)) {
            return response(HttpStatus.BAD_REQUEST,
                    "Your current password is incorrect.");
        }

        String newPassword = request.newPassword();

        if (newPassword.length() < 12 || newPassword.length() > 72) {
            return response(HttpStatus.BAD_REQUEST,
                    "New password must be between 12 and 72 characters.");
        }

        if (passwordEncoder.matches(newPassword, storedHash)) {
            return response(HttpStatus.BAD_REQUEST,
                    "Your new password must differ from your current password.");
        }

        jdbcTemplate.update(
                "UPDATE users SET password_hash = ? WHERE username = ?",
                passwordEncoder.encode(newPassword),
                username);

        return response(HttpStatus.OK,
                "Password changed successfully.");
    }

    private ResponseEntity<Map<String, String>> response(
            HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .body(Map.of("message", message));
    }

    public record CreateAccountRequest(
            String username,
            String email,
            String password) {
    }

    public record ChangePasswordRequest(
            String currentPassword,
            String newPassword) {
    }
}