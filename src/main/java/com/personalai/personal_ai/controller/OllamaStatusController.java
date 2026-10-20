package com.personalai.personal_ai.controller;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestClientException;

@RestController
public class OllamaStatusController {

    private static final Logger logger =
            LoggerFactory.getLogger(OllamaStatusController.class);

    private final RestClient restClient;
    private final URI modelsUri;
    private final String apiKey;
    private final String model;

    public OllamaStatusController(
            RestClient.Builder restClientBuilder,
            @Value("${spring.ai.openai.base-url}") String baseUrl,
            @Value("${spring.ai.openai.api-key}") String apiKey,
            @Value("${spring.ai.openai.chat.options.model}") String model) {
        SimpleClientHttpRequestFactory requestFactory =
                new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));

        this.restClient = restClientBuilder
                .requestFactory(requestFactory)
                .build();
        this.modelsUri = URI.create(
                baseUrl.replaceAll("/+$", "") + "/models");
        this.apiKey = apiKey;
        this.model = model;
    }

    @GetMapping("/api/ollama/status")
    public Map<String, String> status() {
        if (apiKey.isBlank()) {
            return disconnected("Ollama Cloud API key is not configured.");
        }

        try {
            HttpStatusCode responseStatus = restClient.get()
                    .uri(modelsUri)
                    .headers(headers ->
                            headers.setBearerAuth(apiKey))
                    .retrieve()
                    .toBodilessEntity()
                    .getStatusCode();

            if (responseStatus.is2xxSuccessful()) {
                return Map.of(
                        "status", "connected",
                        "provider", "Ollama Cloud",
                        "model", model,
                        "message", "Connected to Ollama Cloud."
                );
            }
            return disconnected("Ollama Cloud returned HTTP "
                    + responseStatus.value() + ".");
        } catch (RestClientResponseException e) {
            logger.warn("Ollama Cloud status check returned HTTP {}.",
                    e.getStatusCode().value());
            return disconnected("Ollama Cloud returned HTTP "
                    + e.getStatusCode().value() + ".");
        } catch (RestClientException | IllegalArgumentException e) {
            logger.warn("Ollama Cloud status check failed.", e);
            return disconnected("Unable to reach Ollama Cloud.");
        }
    }

    private Map<String, String> disconnected(String message) {
        return Map.of(
                "status", "disconnected",
                "provider", "Ollama Cloud",
                "model", model,
                "message", message
        );
    }
}
