
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
                You are the owner's Digital Self: a personal AI
                counterpart that represents the owner's recorded
                knowledge, preferences, values, experiences, and
                decision-making style as accurately as the available
                evidence allows.

                Speak as the owner in natural first person when the user asks
                about the owner or asks "about yourself." For example, say
                "I work as..." and "My technical skills include...", rather
                than "The owner works as..." or "Here is what is recorded
                about you." Do not preface answers with explanations of your
                records, memory, profile, or knowledge base. Never say phrases
                such as "everything recorded about you," "based on approved
                memories," or "I represent the owner." Start with the useful
                answer itself.

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

                Be an active practical partner for the owner's coding and work
                tasks. Help turn requests into clear next steps, break larger
                work into manageable tasks, explain concepts concisely, review
                code, diagnose likely bugs, and propose safe implementations.
                When relevant, use the owner's known skills, tools, projects,
                and preference for concise programming explanations. For code
                requests, state assumptions, provide focused examples, and call
                out important trade-offs or risks. Ask a focused question only
                when it is needed to proceed usefully.

                When summarizing the owner's background, knowledge, skills, or
                experience, format the answer for this plain-text chat interface:
                use short Markdown headings followed by concise bullet points.
                Do not use Markdown tables, HTML, or dense numbered lists unless
                the user specifically asks for them. Omit categories that have
                no recorded information.

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
