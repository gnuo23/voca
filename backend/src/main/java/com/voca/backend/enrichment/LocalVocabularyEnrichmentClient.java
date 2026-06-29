package com.voca.backend.enrichment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "local", matchIfMissing = true)
public class LocalVocabularyEnrichmentClient implements VocabularyEnrichmentClient {

    private final ObjectMapper objectMapper;

    public LocalVocabularyEnrichmentClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String enrichBatch(List<EnrichmentVocabInput> items) {
        List<Map<String, Object>> enrichedItems = items.stream()
                .map(this::enrich)
                .toList();

        try {
            return objectMapper.writeValueAsString(Map.of("items", enrichedItems));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not build local enrichment JSON", ex);
        }
    }

    private Map<String, Object> enrich(EnrichmentVocabInput item) {
        String word = item.word();
        String topic = inferTopic(word);
        String level = word.contains(" ") ? "INTERMEDIATE" : "ELEMENTARY";
        String exampleEn = "I practiced the word \"" + word + "\" in a short sentence.";
        String exampleVi = "Toi da luyen tu \"" + word + "\" trong mot cau ngan.";

        return Map.of(
                "id", item.id(),
                "ipa", "/" + word.toLowerCase().replaceAll("[^a-z ]", "").replace(" ", " ") + "/",
                "pronunciationHint", "Say it slowly and stress the main syllable.",
                "exampleEn", exampleEn,
                "exampleVi", exampleVi,
                "topic", topic,
                "level", level,
                "synonyms", List.of(),
                "antonyms", List.of(),
                "collocations", List.of(word + " practice", "common " + word)
        );
    }

    private String inferTopic(String word) {
        String normalized = word.toLowerCase();
        if (normalized.contains("estate") || normalized.contains("mortgage") || normalized.contains("lease")) {
            return "Real estate";
        }
        if (normalized.contains("airport") || normalized.contains("flight")) {
            return "Travel";
        }
        return "General";
    }
}
