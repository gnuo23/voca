package com.voca.backend.toeic;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "openai")
public class OpenAiToeicChatClient implements ToeicChatClient {

    private final RestClient restClient;
    private final String model;

    public OpenAiToeicChatClient(
            RestClient.Builder restClientBuilder,
            @Value("${app.ai.openai.api-key}") String apiKey,
            @Value("${app.ai.openai.model:gpt-4.1-mini}") String model
    ) {
        this.restClient = restClientBuilder
                .baseUrl("https://api.openai.com/v1")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .build();
        this.model = model;
    }

    @Override
    @SuppressWarnings("unchecked")
    public String chat(String systemPrompt, String userPrompt) {
        Map<String, Object> response = restClient.post()
                .uri("/chat/completions")
                .body(Map.of(
                        "model", model,
                        "messages", List.of(
                                Map.of("role", "system", "content", systemPrompt),
                                Map.of("role", "user", "content", userPrompt)
                        )
                ))
                .retrieve()
                .body(Map.class);

        if (response == null) {
            throw new IllegalStateException("AI response was empty");
        }

        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        if (choices == null || choices.isEmpty()) {
            throw new IllegalStateException("AI response did not include choices");
        }

        Map<String, Object> message = (Map<String, Object>) choices.getFirst().get("message");
        if (message == null || message.get("content") == null) {
            throw new IllegalStateException("AI response did not include message content");
        }

        return message.get("content").toString();
    }
}
