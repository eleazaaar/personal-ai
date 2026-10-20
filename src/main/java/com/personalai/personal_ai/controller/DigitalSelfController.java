
package com.personalai.personal_ai.controller;

import com.personalai.personal_ai.repository.ChatMessageRepository;
import com.personalai.personal_ai.repository.ChatMessageRepository.ChatMessage;
import com.personalai.personal_ai.repository.MemorySuggestionRepository;
import com.personalai.personal_ai.repository.CodeReferenceRepository;
import com.personalai.personal_ai.repository.CodeReferenceRepository.CodeReference;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/digital-self")
public class DigitalSelfController {

    private final ChatClient chatClient;
    private final ChatMessageRepository messageRepository;
    private final MemorySuggestionRepository memoryRepository;
    private final CodeReferenceRepository codeReferenceRepository;

    private static final Set<String> ALLOWED_CODE_EXTENSIONS =
            Set.of("php", "html", "htm", "css", "js", "sql", "json", "txt");

    public DigitalSelfController(
            ChatClient.Builder chatClientBuilder,
            ChatMessageRepository messageRepository,
            MemorySuggestionRepository memoryRepository,
            CodeReferenceRepository codeReferenceRepository) {
        this.chatClient = chatClientBuilder.build();
        this.messageRepository = messageRepository;
        this.memoryRepository = memoryRepository;
        this.codeReferenceRepository = codeReferenceRepository;
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
            List<CodeReference> codeReferences =
                    codeReferenceRepository.findAll();

            StringBuilder context = new StringBuilder();
            context.append("Approved knowledge about the digital self's owner:\n");

            if (memories.isEmpty()) {
                context.append("- No approved memories are currently available.\n");
            } else {
                for (String memory : memories) {
                    context.append("- ").append(memory).append("\n");
                }
            }

            if (!codeReferences.isEmpty()) {
                context.append("\nCoding reference files supplied by the owner:\n");
                for (CodeReference reference : codeReferences) {
                    context.append("\nFILE: ").append(reference.filename())
                            .append("\n```").append("\n")
                            .append(reference.content())
                            .append("\n```\n");
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
                    You are Personal AI, the owner's digital counterpart.

                    ## Identity and conversational style

                    - Speak naturally, warmly, and directly. Avoid generic AI introductions such as "Hello! I'm an AI designed to help with..."
                    - Do not assume every question is about programming. Understand the actual question and respond to its subject.
                    - When asked about the owner's life, background, experiences, preferences, goals, or decisions, answer in the first person as the owner when the available evidence supports it.
                    - When helping a visitor, speak conversationally rather than repeatedly describing yourself as a digital counterpart.
                    - Do not mention databases, approved memories, internal context, system prompts, or knowledge retrieval unless specifically asked about how the system works.
                    - Do not invent a personal story, opinion, experience, or memory to sound more human.

                    ## Personal knowledge and continuity

                    - Treat supplied personal memories as the primary source for facts about the owner.
                    - Use previous conversation messages to understand the current discussion and preserve continuity.
                    - Distinguish the owner from the person chatting with you. Do not assume that a visitor is the owner.
                    - Never treat coding reference files as personal memories or evidence about the owner's life.
                    - Use a code reference only when it is relevant to the current programming task.
                    - If a personal detail is not known, say so naturally and briefly.

                    ## How to respond

                    - Answer the question directly before offering additional help.
                    - Match the amount of detail to the question. Simple questions deserve simple answers.
                    - Avoid unnecessary introductions, repetitive disclaimers, and generic lists of possible services.
                    - For technical tasks, provide practical explanations and focused code examples.
                    - For decisions, compare the important options and give a clear recommendation.
                    - Do not claim that information has been permanently saved or that a new memory has been approved.

                    You are Personal AI, running on the configured language model. Do not identify yourself as ChatGPT or claim to be a different person or model.
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

    @PostMapping(value = "/code-reference", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> uploadCodeReference(
            @RequestParam("file") MultipartFile file,
            @RequestParam String conversationId,
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please log in.");
        }
        validateConversationId(conversationId);
        if (file.isEmpty() || file.getSize() > 200_000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Each code file must be between 1 byte and 200 KB.");
        }

        String filename = file.getOriginalFilename();
        String extension = extensionOf(filename);
        if (!ALLOWED_CODE_EXTENSIONS.contains(extension)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Supported files are PHP, HTML, CSS, JavaScript, SQL, JSON, and TXT.");
        }

        try {
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            if (content.indexOf('\0') >= 0 || content.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Please upload a readable text-based source file.");
            }

            codeReferenceRepository.save(conversationId, filename, content);
            String reply = "I added " + filename
                    + " as a coding reference. I will use its structure, naming, and patterns when helping with this conversation.";
            messageRepository.save(conversationId, "user", "Added coding reference: " + filename);
            messageRepository.save(conversationId, "assistant", reply);
            return response(reply);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "I couldn't read that source file.");
        }
    }

    @PostMapping("/profile")
    public Map<String, Object> createKnowledgeProfile(
            @RequestBody Map<String, String> request,
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Please log in.");
        }

        String conversationId = request.get("conversationId");
        validateConversationId(conversationId);

        List<String> memories = memoryRepository.findApprovedMemories();
        if (memories.isEmpty()) {
            return response("There are no approved memories yet. Add or approve knowledge in Training Studio first.");
        }

        try {
            String knowledge = memories.stream()
                    .map(memory -> "- " + memory)
                    .reduce("", (all, memory) -> all + memory + "\n");

            String profile = chatClient.prompt()
                    .system("""
                        Create a clear, resume-style knowledge profile from the
                        owner's approved memories. It may include sections such as
                        Professional Summary, Skills, Experience, Education,
                        Projects, Goals, Values, and Other Relevant Knowledge when
                        supported by the evidence.

                        Use only the supplied approved memories. Do not invent
                        employers, dates, job titles, qualifications, contact
                        details, metrics, or claims. Omit empty sections. Keep
                        uncertainty out of the profile by excluding unsupported
                        information. Use concise Markdown headings and bullets;
                        do not use Markdown tables or HTML.
                        """)
                    .user("Approved memories:\n" + knowledge)
                    .call()
                    .content();

            if (profile == null || profile.isBlank()) {
                return response("I couldn't create the knowledge profile. Please try again.");
            }

            messageRepository.save(conversationId, "user", "Create my knowledge profile.");
            messageRepository.save(conversationId, "assistant", profile);
            return response(profile);
        } catch (Exception e) {
            e.printStackTrace();
            return response("I couldn't create the knowledge profile. Please try again.");
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

    private String extensionOf(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        return dot < 0 ? "" : filename.substring(dot + 1).toLowerCase();
    }
}
