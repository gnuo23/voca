package com.voca.backend.enrichment;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "gemini")
public class GeminiVocabularyEnrichmentClient implements VocabularyEnrichmentClient {

    private final RestClient restClient;
    private final EnrichmentPromptTemplate promptTemplate;
    private final String apiKey;
    private final String model;

    public GeminiVocabularyEnrichmentClient(
            RestClient.Builder restClientBuilder,
            EnrichmentPromptTemplate promptTemplate,
            @Value("${app.ai.gemini.api-key}") String apiKey,
            @Value("${app.ai.gemini.model:gemini-1.5-flash}") String model
    ) {
        this.restClient = restClientBuilder
                .baseUrl("https://generativelanguage.googleapis.com/v1beta")
                .build();
        this.promptTemplate = promptTemplate;
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    @SuppressWarnings("unchecked")
    public String enrichBatch(List<EnrichmentVocabInput> items) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("GEMINI_API_KEY is required when APP_AI_PROVIDER=gemini");
        }

        Map<String, Object> response = restClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/models/{model}:generateContent")
                        .queryParam("key", apiKey)
                        .build(model))
                .body(Map.of(
                        "contents", List.of(Map.of(
                                "parts", List.of(Map.of("text", promptTemplate.build(items)))
                        )),
                        "generationConfig", Map.of(
                                "responseMimeType", "application/json",
                                "temperature", 0.2
                        )
                ))
                .retrieve()
                .body(Map.class);

        if (response == null) {
            throw new IllegalStateException("Gemini response was empty");
        }

        List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
        if (candidates == null || candidates.isEmpty()) {
            throw new IllegalStateException("Gemini response did not include candidates");
        }

        Map<String, Object> content = (Map<String, Object>) candidates.getFirst().get("content");
        if (content == null) {
            throw new IllegalStateException("Gemini response did not include content");
        }

        List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
        if (parts == null || parts.isEmpty() || parts.getFirst().get("text") == null) {
            throw new IllegalStateException("Gemini response did not include text");
        }

        return parts.getFirst().get("text").toString();
    }
}
