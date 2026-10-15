package com.personalai.personal_ai.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.security.core.Authentication;

@Controller
public class HomeController {

    @GetMapping("/")
    @ResponseBody
    public String home(Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_ADMIN"));
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Personal AI</title>
                    <style>
                        :root {
                            color-scheme: dark;
                            --bg: #111318;
                            --panel: #191c24;
                            --border: #2c303b;
                            --text: #f3f4f8;
                            --muted: #969cad;
                            --accent: #8b78ff;
                            --user: #5142b5;
                        }

                        * { box-sizing: border-box; }

                        body {
                            margin: 0;
                            min-height: 100vh;
                            background: var(--bg);
                            color: var(--text);
                            font-family: Inter, system-ui, -apple-system, BlinkMacSystemFont,
                                         "Segoe UI", sans-serif;
                        }

                        .app {
                            display: flex;
                            flex-direction: column;
                            max-width: 1000px;
                            height: 100vh;
                            height: 100dvh;
                            margin: 0 auto;
                        }

                        header {
                            display: flex;
                            align-items: center;
                            gap: 12px;
                            padding: 18px 24px;
                            border-bottom: 1px solid var(--border);
                            background: var(--panel);
                        }

                        .logo {
                            display: grid;
                            place-items: center;
                            width: 42px;
                            height: 42px;
                            border-radius: 13px;
                            background: var(--accent);
                            color: white;
                            font-size: 21px;
                            font-weight: 700;
                        }

                        .brand h1 {
                            margin: 0;
                            font-size: 17px;
                            font-weight: 650;
                        }

                        .brand p {
                            margin: 4px 0 0;
                            color: var(--muted);
                            font-size: 12px;
                        }

                        .status {
                            margin-left: auto;
                            color: #9ce4b2;
                            font-size: 12px;
                        }

                        .status::before {
                            content: "";
                            display: inline-block;
                            width: 7px;
                            height: 7px;
                            margin-right: 7px;
                            border-radius: 50%;
                            background: #69d991;
                        }

                        #messages {
                            flex: 1;
                            overflow-y: auto;
                            padding: 28px 24px;
                            scroll-behavior: smooth;
                        }

                        .welcome {
                            max-width: 540px;
                            margin: 12vh auto 0;
                            text-align: center;
                        }

                        .welcome-icon {
                            display: grid;
                            place-items: center;
                            width: 64px;
                            height: 64px;
                            margin: 0 auto 18px;
                            border-radius: 20px;
                            background: #27223f;
                            color: #c3b8ff;
                            font-size: 30px;
                        }

                        .welcome h2 {
                            margin: 0 0 10px;
                            font-size: clamp(23px, 4vw, 31px);
                        }

                        .welcome p {
                            margin: 0;
                            color: var(--muted);
                            line-height: 1.7;
                        }

                        .suggestions {
                            display: flex;
                            justify-content: center;
                            flex-wrap: wrap;
                            gap: 8px;
                            margin-top: 24px;
                        }

                        .suggestion {
                            padding: 10px 13px;
                            border: 1px solid var(--border);
                            border-radius: 12px;
                            background: var(--panel);
                            color: var(--text);
                            cursor: pointer;
                            font: inherit;
                            font-size: 13px;
                        }

                        .suggestion:hover {
                            border-color: var(--accent);
                        }

                        .message {
                            display: flex;
                            flex-direction: column;
                            gap: 7px;
                            margin: 0 auto 24px;
                            max-width: 760px;
                        }

                        .message.user { align-items: flex-end; }
                        .message.assistant { align-items: flex-start; }

                        .speaker {
                            color: var(--muted);
                            font-size: 12px;
                        }

                        .bubble {
                            max-width: min(85%, 680px);
                            padding: 13px 16px;
                            border: 1px solid var(--border);
                            border-radius: 16px;
                            background: var(--panel);
                            line-height: 1.7;
                            overflow-wrap: anywhere;
                            white-space: pre-wrap;
                        }

                        .user .bubble {
                            border-color: transparent;
                            background: var(--user);
                        }

                        .error .bubble {
                            border-color: #743b45;
                            color: #ffb8c0;
                        }

                        .composer-area {
                            padding: 16px 24px 20px;
                            border-top: 1px solid var(--border);
                            background: var(--bg);
                        }

                        .composer {
                            display: flex;
                            align-items: flex-end;
                            gap: 12px;
                            max-width: 760px;
                            margin: 0 auto;
                            padding: 10px;
                            border: 1px solid #383d4a;
                            border-radius: 17px;
                            background: var(--panel);
                        }

                        textarea {
                            flex: 1;
                            width: 100%;
                            max-height: 180px;
                            min-height: 42px;
                            padding: 10px;
                            resize: none;
                            border: 0;
                            outline: none;
                            background: transparent;
                            color: var(--text);
                            font: inherit;
                            line-height: 1.5;
                        }

                        textarea::placeholder { color: var(--muted); }

                        #send {
                            display: grid;
                            place-items: center;
                            flex-shrink: 0;
                            width: 42px;
                            height: 42px;
                            border: 0;
                            border-radius: 12px;
                            background: var(--accent);
                            color: white;
                            cursor: pointer;
                            font-size: 19px;
                        }

                        #send:disabled {
                            cursor: not-allowed;
                            opacity: .5;
                        }

                        .hint {
                            margin: 10px 0 0;
                            color: var(--muted);
                            font-size: 11px;
                            text-align: center;
                        }

                        .memory-card {
                            width: 100%;
                            max-width: 680px;
                            margin: 0 auto 24px;
                            padding: 16px;
                            border: 1px solid #48405f;
                            border-radius: 14px;
                            background: #1d1b29;
                        }

                        .memory-card h3 {
                            margin: 0 0 8px;
                            font-size: 14px;
                        }

                        .memory-card .memory-text {
                            margin-bottom: 10px;
                            line-height: 1.6;
                            overflow-wrap: anywhere;
                        }

                        .memory-card .memory-type {
                            margin-bottom: 14px;
                            color: var(--muted);
                            font-size: 12px;
                        }

                        .memory-actions {
                            display: flex;
                            flex-wrap: wrap;
                            gap: 8px;
                        }

                        .memory-actions button {
                            padding: 9px 13px;
                            border: 1px solid var(--border);
                            border-radius: 9px;
                            background: var(--panel);
                            color: var(--text);
                            cursor: pointer;
                        }

                        .memory-actions .approve-memory {
                            border-color: #437957;
                            background: #244832;
                        }

                        .memory-actions button:disabled {
                            cursor: not-allowed;
                            opacity: .6;
                        }

                        .memory-result {
                            margin-top: 10px;
                            color: #9ce4b2;
                            font-size: 12px;
                        }

                        @media (max-width: 600px) {
                            header { padding: 14px 16px; }
                            #messages { padding: 22px 15px; }
                            .composer-area { padding: 12px 12px 15px; }
                            .bubble { max-width: 92%; }
                            .welcome { margin-top: 8vh; }
                        }

                        .tabs {
                            display: flex;
                            gap: 8px;
                            padding: 12px 24px;
                            border-bottom: 1px solid var(--border);
                            background: var(--panel);
                            overflow-x: auto;
                        }

                        .tab {
                            flex-shrink: 0;
                            padding: 10px 14px;
                            border: 1px solid transparent;
                            border-radius: 10px;
                            background: transparent;
                            color: var(--muted);
                            cursor: pointer;
                            font: inherit;
                            font-size: 13px;
                        }

                        .tab:hover {
                            color: var(--text);
                            background: #242733;
                        }

                        .tab.active {
                            border-color: #48405f;
                            background: #27223f;
                            color: #c3b8ff;
                        }

                        @media (max-width: 600px) {
                            .tabs {
                                padding: 10px 12px;
                            }
                        }
                    </style>
                </head>
                <body>
                    <div class="app">
                        <header>
                            <div class="logo">✳</div>
                            <div class="brand">
                                <h1>Personal AI</h1>
                                <p>Your personal AI companion</p>
                            </div>
                            <div class="status">Connected</div>
                            
                            <form id="logoutForm" action="/logout" method="post">
                                <input type="hidden" id="logoutCsrf" name="_csrf" value="">
                                <button type="submit" class="logout-btn">Logout</button>
                            </form>
                        </header>

                        <nav class="tabs" aria-label="Main navigation">
                            <button class="tab active" data-mode="digital_self">
                                My Digital Self
                            </button>
                            __ADMIN_TABS__
                        </nav>

                        <main id="messages" aria-live="polite">
                            <section class="welcome" id="welcome">
                                <div class="welcome-icon">✳</div>
                                <h2>What would you like to talk about?</h2>
                                <p>Your personal AI is ready to help you think,
                                   plan, learn, and organize your ideas.</p>
                                <div class="suggestions">
                                    <button class="suggestion" data-message="Introduce yourself and explain how you can help me.">Meet your AI</button>
                                    <button class="suggestion" data-message="Help me organize my goals for this week.">Organize my goals</button>
                                    <button class="suggestion" data-message="What kinds of things could you help me remember?">Explore personal memory</button>
                                </div>
                            </section>
                        </main>

                        <div class="composer-area">
                            <form class="composer" id="chatForm">
                                <textarea
                                    id="messageInput"
                                    placeholder="Message your Personal AI..."
                                    rows="1"
                                    maxlength="10000"
                                    aria-label="Your message"
                                    required></textarea>
                                <button id="send" type="submit" aria-label="Send message">➤</button>
                            </form>
                            <p class="hint">Enter to send · Shift + Enter for a new line</p>
                        </div>
                    </div>

                    <script>
                        const form = document.getElementById("chatForm");
                        const input = document.getElementById("messageInput");
                        const sendButton = document.getElementById("send");
                        const messages = document.getElementById("messages");
                        const welcome = document.getElementById("welcome");
                        let chatMode = "digital_self";

                        const modeFragments = {
                            digital_self: document.createDocumentFragment(),
                            training: document.createDocumentFragment()
                        };
                        const loadedHistoryModes = new Set();
                        const loadingHistoryModes = new Set();

                        let displayedMode = "digital_self";

                        async function getCsrfHeaders() {
                            const response = await fetch("/api/csrf", {
                                method: "GET",
                                headers: { "Accept": "application/json" }
                            });

                            if (!response.ok) {
                                throw new Error("Unable to obtain the security token.");
                            }

                            const data = await response.json();

                            return {
                                "X-XSRF-TOKEN": data.token
                            };
                        }

                        function getConversationId(mode) {
                            const key = "personalAiConversationId_" + mode;
                            let id = sessionStorage.getItem(key);

                            if (!id) {
                                id = crypto.randomUUID();
                                sessionStorage.setItem(key, id);
                            }

                            return id;
                        }

                        let conversationId = getConversationId(chatMode);

                        function addMessage(role, text, isError = false) {
                            const wrapper = createMessage(role, text, isError);
                            messages.appendChild(wrapper);
                            messages.scrollTop = messages.scrollHeight;
                            return wrapper;
                        }

                        function createMessage(role, text, isError = false) {
                            const wrapper = document.createElement("div");
                            wrapper.className = "message " + role +
                                (isError ? " error" : "");

                            const speaker = document.createElement("div");
                            speaker.className = "speaker";
                            speaker.textContent = role === "user" ? "You" : "Personal AI";

                            const bubble = document.createElement("div");
                            bubble.className = "bubble";
                            bubble.textContent = text;

                            wrapper.append(speaker, bubble);
                            return wrapper;
                        }

                        async function loadConversationHistory(mode) {
                            if (loadedHistoryModes.has(mode) || loadingHistoryModes.has(mode)) {
                                return;
                            }

                            loadingHistoryModes.add(mode);

                            try {
                                const id = getConversationId(mode);
                                const endpoint = mode === "training"
                                    ? "/api/training/history"
                                    : "/api/digital-self/history";
                                const response = await fetch(
                                    endpoint + "?conversationId=" + encodeURIComponent(id),
                                    { headers: { "Accept": "application/json" } }
                                );

                                if (!response.ok) {
                                    throw new Error("Unable to load this conversation.");
                                }

                                const history = await response.json();
                                if (!Array.isArray(history)) {
                                    throw new Error("The server returned an unexpected history response.");
                                }

                                const container = mode === displayedMode
                                    ? messages
                                    : modeFragments[mode];
                                container.querySelector?.("#welcome")?.remove();

                                history.forEach(turn => {
                                    if (turn.role === "user" || turn.role === "assistant") {
                                        container.appendChild(createMessage(turn.role, turn.content));
                                    }
                                });

                                loadedHistoryModes.add(mode);
                                if (mode === displayedMode) {
                                    messages.scrollTop = messages.scrollHeight;
                                }
                            } catch (error) {
                                console.error("Conversation history could not be loaded:", error);
                            } finally {
                                loadingHistoryModes.delete(mode);
                            }
                        }

                        async function sendMessage(message) {
                            message = message.trim();
                            if (!message || sendButton.disabled) return;

                            messages.querySelector("#welcome")?.remove();
                            addMessage("user", message);
                            input.value = "";
                            input.style.height = "42px";
                            sendButton.disabled = true;

                            const loading = addMessage("assistant", "Thinking...");

                            try {
                                const endpoint = chatMode === "training" ? "/api/training" : "/api/digital-self";
                                const response = await fetch(endpoint, {
                                    method: "POST",
                                    headers: {
                                        "Content-Type": "application/json",
                                        "Accept": "application/json",
                                        ...await getCsrfHeaders()
                                    },
                                    body: JSON.stringify({
                                        conversationId: conversationId,
                                        message: message
                                    })
                                });

                                if (!response.ok) {
                                    throw new Error("The AI request failed (HTTP " +
                                        response.status + "). Please try again.");
                                }

                                const data = await response.json();
                                loading.remove();

                                if (typeof data.reply !== "string") {
                                    throw new Error("The server returned an unexpected response.");
                                }

                                addMessage("assistant", data.reply);
                                renderMemorySuggestions(data.suggestions);
                            } catch (error) {
                                loading.remove();
                                addMessage("assistant",
                                    error.message || "Unable to contact the AI. Please try again.",
                                    true);
                            } finally {
                                sendButton.disabled = false;
                                input.focus();
                            }
                        }

                        function renderMemorySuggestions(suggestions) {
                            if (!Array.isArray(suggestions)) return;

                            suggestions.forEach(suggestion => {
                                const card = document.createElement("section");
                                card.className = "memory-card";

                                const heading = document.createElement("h3");
                                heading.textContent = "Suggested memory — review required";

                                const memoryText = document.createElement("div");
                                memoryText.className = "memory-text";
                                memoryText.textContent = suggestion.memoryText;

                                const type = document.createElement("div");
                                type.className = "memory-type";
                                type.textContent = "Category: " + suggestion.memoryType;

                                const actions = document.createElement("div");
                                actions.className = "memory-actions";

                                const approve = document.createElement("button");
                                approve.className = "approve-memory";
                                approve.textContent = "Approve";

                                const reject = document.createElement("button");
                                reject.textContent = "Reject";

                                const result = document.createElement("div");
                                result.className = "memory-result";
                                result.setAttribute("aria-live", "polite");

                                async function reviewMemory(action) {
                                    approve.disabled = true;
                                    reject.disabled = true;
                                    result.textContent = "Processing...";

                                    try {
                                        const response = await fetch(
                                            "/api/memories/" +
                                            encodeURIComponent(suggestion.id) +
                                            "/" + action,
                                            {
                                                method: "POST",
                                                headers: {
                                                    "Accept": "application/json",
                                                    ...await getCsrfHeaders()
                                                }
                                            }
                                        );

                                        const data = await response.json();

                                        if (!response.ok) {
                                            throw new Error(
                                                data.message || "Unable to review this memory."
                                            );
                                        }

                                        result.textContent = action === "approve"
                                            ? "Memory approved and saved."
                                            : "Suggestion rejected.";

                                        actions.remove();
                                    } catch (error) {
                                        result.textContent =
                                            error.message || "Unable to contact the server.";

                                        approve.disabled = false;
                                        reject.disabled = false;
                                    }
                                }

                                approve.addEventListener("click", () => reviewMemory("approve"));
                                reject.addEventListener("click", () => reviewMemory("reject"));

                                actions.append(approve, reject);
                                card.append(heading, memoryText, type, actions, result);
                                messages.appendChild(card);
                            });

                            messages.scrollTop = messages.scrollHeight;
                        }

                        form.addEventListener("submit", event => {
                            event.preventDefault();
                            sendMessage(input.value);
                        });

                        input.addEventListener("input", () => {
                            input.style.height = "42px";
                            input.style.height = Math.min(input.scrollHeight, 180) + "px";
                        });

                        input.addEventListener("keydown", event => {
                            if (event.key === "Enter" && !event.shiftKey) {
                                event.preventDefault();
                                form.requestSubmit();
                            }
                        });

                        document.querySelectorAll(".suggestion").forEach(button => {
                            button.addEventListener("click", () => {
                                sendMessage(button.dataset.message);
                            });
                        });

                        const tabs = document.querySelectorAll(".tab");
                        const welcomeTitle = document.querySelector("#welcome h2");
                        const welcomeDescription = document.querySelector("#welcome p");
                        const messageInput = document.getElementById("messageInput");

                        function switchMode(mode) {
                            if (mode !== "training" && mode !== "digital_self") return;

                            // Prevent switching while an AI request is in progress.
                            if (sendButton.disabled && mode !== displayedMode) return;

                            // Save the currently displayed conversation.
                            if (mode !== displayedMode) {
                                while (messages.firstChild) {
                                    modeFragments[displayedMode].appendChild(messages.firstChild);
                                }

                                // Restore the selected mode's saved conversation.
                                messages.appendChild(modeFragments[mode]);
                                displayedMode = mode;
                            }

                            chatMode = mode;
                            conversationId = getConversationId(mode);

                            loadConversationHistory(mode);

                            tabs.forEach(tab => {
                                const active = tab.dataset.mode === mode;
                                tab.classList.toggle("active", active);
                                tab.setAttribute("aria-pressed", String(active));
                            });

                            // Show a welcome section for a new or empty conversation.
                            if (!messages.querySelector(".message, .memory-card")) {
                                let currentWelcome = messages.querySelector("#welcome");

                                if (!currentWelcome) {
                                    currentWelcome = document.createElement("section");
                                    currentWelcome.id = "welcome";
                                    currentWelcome.innerHTML = "<h2></h2><p></p>";
                                    messages.appendChild(currentWelcome);
                                }

                                const title = currentWelcome.querySelector("h2");
                                const description = currentWelcome.querySelector("p");

                                if (mode === "training") {
                                    title.textContent = "Training Studio";
                                    description.textContent =
                                        "Teach your AI about your preferences, experiences, goals, and values. Review memory suggestions before saving them.";

                                    input.placeholder = "Tell your AI something about yourself...";
                                } else {
                                    title.textContent = "My Digital Self";
                                    description.textContent =
                                        "Chat with your digital self using your approved memories to help answer questions and evaluate decisions.";

                                    input.placeholder = "Talk to your Digital Self...";
                                }
                            }

                            messages.scrollTop = messages.scrollHeight;
                        }

                        tabs.forEach(tab => {
                            tab.addEventListener("click", () => {
                                switchMode(tab.dataset.mode);
                            });
                        });

                        switchMode("digital_self");

                        async function prepareLogout() {
                            try {
                                const response = await fetch('/api/csrf', {
                                    credentials: 'same-origin'
                                });

                                if (!response.ok) {
                                    throw new Error('Could not retrieve CSRF token.');
                                }

                                const data = await response.json();
                                document.getElementById('logoutCsrf').value = data.token;
                            } catch (error) {
                                console.error('Logout setup failed:', error);
                            }
                        }

                        prepareLogout();
                    </script>
                </body>
                </html>
                """.replace(
                "__ADMIN_TABS__",
                        isAdmin ? """
                            <button class="tab" data-mode="training">
                                Training Studio
                            </button>
                            """ : ""
                );
    }
}
