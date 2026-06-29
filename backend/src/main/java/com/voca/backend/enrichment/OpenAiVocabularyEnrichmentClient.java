package com.voca.backend.enrichment;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "openai")
public class OpenAiVocabularyEnrichmentClient implements VocabularyEnrichmentClient {

    private final RestClient restClient;
    private final EnrichmentPromptTemplate promptTemplate;
    private final String model;

    public OpenAiVocabularyEnrichmentClient(
            RestClient.Builder restClientBuilder,
            EnrichmentPromptTemplate promptTemplate,
            @Value("${app.ai.openai.api-key}") String apiKey,
            @Value("${app.ai.openai.model:gpt-4.1-mini}") String model
    ) {
        this.restClient = restClientBuilder
                .baseUrl("https://api.openai.com/v1")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .build();
        this.promptTemplate = promptTemplate;
        this.model = model;
    }

    @Override
    @SuppressWarnings("unchecked")
    public String enrichBatch(List<EnrichmentVocabInput> items) {
        Map<String, Object> response = restClient.post()
                .uri("/chat/completions")
                .body(Map.of(
                        "model", model,
                        "response_format", Map.of("type", "json_object"),
                        "messages", List.of(
                                Map.of("role", "system", "content", "You return valid JSON only."),
                                Map.of("role", "user", "content", promptTemplate.build(items))
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
