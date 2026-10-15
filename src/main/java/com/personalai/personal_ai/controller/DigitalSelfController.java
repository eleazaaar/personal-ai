
package com.personalai.personal_ai.controller;

import com.personalai.personal_ai.repository.ChatMessageRepository;
import com.personalai.personal_ai.repository.ChatMessageRepository.ChatMessage;
import com.personalai.personal_ai.repository.MemorySuggestionRepository;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/digital-self")
public class DigitalSelfController {

    private final ChatClient chatClient;
    private final ChatMessageRepository messageRepository;
    private final MemorySuggestionRepository memoryRepository;

    public DigitalSelfController(
            ChatClient.Builder chatClientBuilder,
            ChatMessageRepository messageRepository,
            MemorySuggestionRepository memoryRepository) {
        this.chatClient = chatClientBuilder.build();
        this.messageRepository = messageRepository;
        this.memoryRepository = memoryRepository;
    }

    @GetMapping("/history")
    public List<ChatMessage> history(
            @RequestParam String conversationId,
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Please log in.");
        }

        validateConversationId(conversationId);
        return messageRepository.findByConversationId(conversationId);
    }

    @PostMapping
    public Map<String, Object> chat(
            @RequestBody Map<String, String> request,
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Please log in.");
        }

        String message = request.get("message");
        String conversationId = request.get("conversationId");

        if (message == null || message.isBlank()) {
            return response("Please enter a message.");
        }

        if (message.length() > 10000) {
            return response("Your message is too long.");
        }

        if (conversationId == null || conversationId.isBlank()
                || conversationId.length() > 100) {
            return response(
                    "Invalid conversation ID. Please start a new chat.");
        }

        try {
            List<ChatMessage> history =
                    messageRepository.findByConversationId(conversationId);

            List<String> memories =
                    memoryRepository.findApprovedMemories();

            StringBuilder context = new StringBuilder();
            context.append("Approved knowledge about the digital self's owner:\n");

            if (memories.isEmpty()) {
                context.append("- No approved memories are currently available.\n");
            } else {
                for (String memory : memories) {
                    context.append("- ").append(memory).append("\n");
                }
            }

            context.append("\nDigital Self conversation history:\n");

            for (ChatMessage turn : history) {
                context.append("user".equals(turn.role())
                                ? "User: " : "Digital Self: ")
                        .append(turn.content())
                        .append("\n\n");
            }

            context.append("Current user message:\n").append(message);

            String systemPrompt = """
                You are the owner's Digital Self: a personal AI
                counterpart that represents the owner's recorded
                knowledge, preferences, values, experiences, and
                decision-making style as accurately as the available
                evidence allows.

                Use the approved knowledge provided to you when relevant.
                Distinguish recorded facts from assumptions. Never invent
                the owner's experiences, opinions, or preferences. If the
                knowledge does not establish what the owner would think,
                say so clearly.

                For questions that require a decision:
                1. Identify the goal and important constraints.
                2. Consider relevant approved knowledge.
                3. Compare plausible options and trade-offs.
                4. Check for contradictions or missing information.
                5. Give a clear recommendation with concise reasons.
                6. State uncertainty when evidence is insufficient.

                Think carefully before answering, but provide a clear
                explanation rather than private hidden reasoning or an
                exhaustive internal thought process.

                Be helpful, logical, precise, and consistent. Ask a focused
                follow-up question if essential information is missing.
                Do not claim that new information has been permanently saved.
                Do not create training-memory suggestions.

                You are Personal AI, powered through Ollama Cloud.
                Do not identify yourself as ChatGPT.
                """;

            String reply = chatClient.prompt()
                    .system(systemPrompt)
                    .user(context.toString())
                    .call()
                    .content();

            if (reply == null || reply.isBlank()) {
                return response(
                        "I couldn't generate a response. Please try again.");
            }

            messageRepository.save(conversationId, "user", message);
            messageRepository.save(conversationId, "assistant", reply);

            return response(reply);

        } catch (Exception e) {
            e.printStackTrace();
            return response(
                    "I couldn't process your message. Please check the application logs and try again.");
        }
    }

    private Map<String, Object> response(String reply) {
        return Map.of("reply", reply, "suggestions", List.of());
    }

    private void validateConversationId(String conversationId) {
        if (conversationId == null || conversationId.isBlank()
                || conversationId.length() > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid conversation ID.");
        }
    }
}
