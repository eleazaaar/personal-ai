package com.personalai.personal_ai.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class LoginController {

    @GetMapping("/login")
    @ResponseBody
    public String login(
        org.springframework.security.web.csrf.CsrfToken csrfToken) {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <title>Personal AI - Login</title>
                <style>
                    body {
                        margin: 0;
                        min-height: 100vh;
                        display: grid;
                        place-items: center;
                        background: #111318;
                        color: #f3f4f8;
                        font-family: Arial, sans-serif;
                    }
                    .panel {
                        width: min(360px, calc(100% - 40px));
                        padding: 28px;
                        background: #191c24;
                        border: 1px solid #2c303b;
                        border-radius: 16px;
                        box-sizing: border-box;
                    }
                    h1 { font-size: 24px; }
                    p { color: #969cad; }
                    label { display: block; margin-top: 16px; }
                    input {
                        width: 100%;
                        box-sizing: border-box;
                        margin-top: 7px;
                        padding: 12px;
                        border-radius: 8px;
                        border: 1px solid #2c303b;
                        background: #111318;
                        color: #f3f4f8;
                    }
                    button {
                        width: 100%;
                        margin-top: 22px;
                        padding: 12px;
                        border: 0;
                        border-radius: 8px;
                        background: #8b78ff;
                        color: white;
                        font-weight: bold;
                        cursor: pointer;
                    }
                    .error { color: #ff8585; }
                    .success { color: #8ee0aa; }
                </style>
            </head>
            <body>
                <main class="panel">
                    <h1>Personal AI</h1>
                    <p>Sign in to your personal assistant.</p>

                    <form method="post" action="/login">
                        <input type="hidden" name="_csrf" value="__CSRF_TOKEN__">

                        <label for="username">Username</label>
                        <input id="username" name="username"
                               autocomplete="username" required>

                        <label for="password">Password</label>
                        <input id="password" name="password"
                               type="password"
                               autocomplete="current-password" required>

                        <button type="submit">Sign in</button>
                    </form>

                    <p class="error" id="errorMessage" hidden>
                        Invalid username or password.
                    </p>
                    <p class="success" id="logoutMessage" hidden>
                        You have been logged out.
                    </p>
                    <script>
                        const params = new URLSearchParams(location.search);
                        if (params.has('error')) {
                            document.getElementById('errorMessage').hidden = false;
                        }
                        if (params.has('logout')) {
                            document.getElementById('logoutMessage').hidden = false;
                        }
                    </script>
                </main>
            </body>
            </html>
            """.replace("__CSRF_TOKEN__", csrfToken.getToken());
    }
}