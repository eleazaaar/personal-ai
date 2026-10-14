package com.personalai.personal_ai.controller;

import com.personalai.personal_ai.repository.MemorySuggestionRepository;
import com.personalai.personal_ai.repository.MemorySuggestionRepository.MemorySuggestion;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/memories")
public class MemoryController {

    private final MemorySuggestionRepository memoryRepository;

    public MemoryController(
            MemorySuggestionRepository memoryRepository) {
        this.memoryRepository = memoryRepository;
    }

    // List all suggestions awaiting approval.
    @GetMapping("/pending")
    public List<MemorySuggestion> getPendingMemories() {
        return memoryRepository.findPending();
    }

    // Approve a suggestion and save it as a permanent memory.
    @PostMapping("/{id}/approve")
    @Transactional
    public ResponseEntity<Map<String, Object>> approveMemory(
            @PathVariable long id) {

        if (id <= 0) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "Invalid memory suggestion ID.")
            );
        }

        MemorySuggestion suggestion = memoryRepository.findById(id)
                .orElse(null);

        if (suggestion == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    Map.of("message", "Memory suggestion not found.")
            );
        }

        if (!"pending".equals(suggestion.status())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    Map.of(
                            "message",
                            "This suggestion has already been reviewed.",
                            "status",
                            suggestion.status()
                    )
            );
        }

        memoryRepository.saveApprovedMemory(suggestion);

        int updated = memoryRepository.updateStatus(id, "approved");

        if (updated != 1) {
            throw new IllegalStateException(
                    "Could not update memory suggestion status."
            );
        }

        return ResponseEntity.ok(
                Map.of(
                        "message", "Memory approved and saved.",
                        "id", id,
                        "memory", suggestion.memoryText(),
                        "status", "approved"
                )
        );
    }

    // Reject a suggestion without saving it as a permanent memory.
    @PostMapping("/{id}/reject")
    @Transactional
    public ResponseEntity<Map<String, Object>> rejectMemory(
            @PathVariable long id) {

        if (id <= 0) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "Invalid memory suggestion ID.")
            );
        }

        MemorySuggestion suggestion = memoryRepository.findById(id)
                .orElse(null);

        if (suggestion == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    Map.of("message", "Memory suggestion not found.")
            );
        }

        if (!"pending".equals(suggestion.status())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    Map.of(
                            "message",
                            "This suggestion has already been reviewed.",
                            "status",
                            suggestion.status()
                    )
            );
        }

        int updated = memoryRepository.updateStatus(id, "rejected");

        if (updated != 1) {
            throw new IllegalStateException(
                    "Could not reject memory suggestion."
            );
        }

        return ResponseEntity.ok(
                Map.of(
                        "message", "Memory suggestion rejected.",
                        "id", id,
                        "status", "rejected"
                )
        );
    }
}