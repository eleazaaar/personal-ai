
package com.personalai.personal_ai.controller;

import com.personalai.personal_ai.repository.PersonalMemoryRepository;
import com.personalai.personal_ai.repository.PersonalMemoryRepository.PersonalMemory;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/knowledge")
@PreAuthorize("hasRole('ADMIN')")
public class MemoryManagementController {

    private final PersonalMemoryRepository memoryRepository;

    private static final Set<String> ALLOWED_TYPES =
            Set.of("fact", "preference", "episodic", "inference", "other");

    public MemoryManagementController(
            PersonalMemoryRepository memoryRepository) {
        this.memoryRepository = memoryRepository;
    }

    @GetMapping
    public ResponseEntity<?> getAllMemories() {
        return ResponseEntity.ok(memoryRepository.findAll());
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> createMemory(
            @RequestBody MemoryRequest request) {

        String validationError = validate(request);
        if (validationError != null) {
            return error(HttpStatus.BAD_REQUEST, validationError);
        }

        memoryRepository.create(
                request.memoryText().trim(),
                request.memoryType().trim().toLowerCase()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(
                Map.of("message", "Memory added successfully.")
        );
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> updateMemory(
            @PathVariable long id,
            @RequestBody MemoryRequest request) {

        if (id <= 0 || memoryRepository.findById(id).isEmpty()) {
            return error(HttpStatus.NOT_FOUND, "Memory not found.");
        }

        String validationError = validate(request);
        if (validationError != null) {
            return error(HttpStatus.BAD_REQUEST, validationError);
        }

        memoryRepository.update(
                id,
                request.memoryText().trim(),
                request.memoryType().trim().toLowerCase()
        );

        return ResponseEntity.ok(
                Map.of("message", "Memory updated successfully.")
        );
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> deleteMemory(@PathVariable long id) {
        if (id <= 0) {
            return error(HttpStatus.BAD_REQUEST, "Invalid memory ID.");
        }

        if (memoryRepository.delete(id) != 1) {
            return error(HttpStatus.NOT_FOUND, "Memory not found.");
        }

        return ResponseEntity.ok(
                Map.of("message", "Memory deleted successfully.")
        );
    }

    private String validate(MemoryRequest request) {
        if (request == null
                || request.memoryText() == null
                || request.memoryType() == null) {
            return "Memory text and memory type are required.";
        }

        String memoryText = request.memoryText().trim();
        String memoryType = request.memoryType().trim().toLowerCase();

        if (memoryText.isBlank()) {
            return "Memory text cannot be empty.";
        }

        if (memoryText.length() > 1000) {
            return "Memory text must not exceed 1000 characters.";
        }

        if (!ALLOWED_TYPES.contains(memoryType)) {
            return "Invalid memory type. Use fact, preference, episodic, inference, or other.";
        }

        return null;
    }

    private ResponseEntity<Map<String, String>> error(
            HttpStatus status, String message) {
        return ResponseEntity.status(status).body(
                Map.of("message", message)
        );
    }

    public record MemoryRequest(String memoryText, String memoryType) {}
}