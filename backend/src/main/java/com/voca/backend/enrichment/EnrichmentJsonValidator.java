package com.voca.backend.enrichment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class EnrichmentJsonValidator {

    private static final Set<String> ALLOWED_LEVELS = Set.of(
            "BEGINNER",
            "ELEMENTARY",
            "INTERMEDIATE",
            "UPPER_INTERMEDIATE",
            "ADVANCED"
    );

    private final ObjectMapper objectMapper;

    public EnrichmentJsonValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ValidatedEnrichmentBatch validate(String rawJson, List<Long> expectedIds) {
        JsonNode root = readJson(rawJson);
        JsonNode itemsNode = root.get("items");
        if (itemsNode == null || !itemsNode.isArray()) {
            throw new IllegalArgumentException("AI JSON must include an items array");
        }
        if (itemsNode.size() != expectedIds.size()) {
            throw new IllegalArgumentException("AI JSON item count does not match the request");
        }

        Set<Long> expected = new HashSet<>(expectedIds);
        Set<Long> seen = new HashSet<>();
        List<EnrichedVocabItem> items = new ArrayList<>();
        for (JsonNode itemNode : itemsNode) {
            EnrichedVocabItem item = parseItem(itemNode);
            if (!expected.contains(item.id())) {
                throw new IllegalArgumentException("AI JSON returned an unknown item id: " + item.id());
            }
            if (!seen.add(item.id())) {
                throw new IllegalArgumentException("AI JSON returned duplicate item id: " + item.id());
            }
            items.add(item);
        }

        return new ValidatedEnrichmentBatch(items);
    }

    private JsonNode readJson(String rawJson) {
        try {
            return objectMapper.readTree(rawJson);
        } catch (IOException ex) {
            throw new IllegalArgumentException("AI returned invalid JSON", ex);
        }
    }

    private EnrichedVocabItem parseItem(JsonNode itemNode) {
        Long id = requiredLong(itemNode, "id");
        String level = requiredText(itemNode, "level", 40);
        if (!ALLOWED_LEVELS.contains(level)) {
            throw new IllegalArgumentException("AI JSON returned unsupported level for item " + id);
        }

        return new EnrichedVocabItem(
                id,
                requiredText(itemNode, "ipa", 120),
                requiredText(itemNode, "pronunciationHint", 500),
                requiredText(itemNode, "exampleEn", 1000),
                requiredText(itemNode, "exampleVi", 1000),
                requiredText(itemNode, "topic", 120),
                level,
                requiredTextArray(itemNode, "synonyms", 20, 120),
                requiredTextArray(itemNode, "antonyms", 20, 120),
                requiredTextArray(itemNode, "collocations", 20, 160)
        );
    }

    private Long requiredLong(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.canConvertToLong()) {
            throw new IllegalArgumentException("AI JSON item is missing numeric field: " + field);
        }
        return value.asLong();
    }

    private String requiredText(JsonNode node, String field, int maxLength) {
        JsonNode value = node.get(field);
        if (value == null || !value.isTextual() || value.asText().isBlank()) {
            throw new IllegalArgumentException("AI JSON item is missing text field: " + field);
        }
        String text = value.asText().trim();
        if (text.length() > maxLength) {
            throw new IllegalArgumentException("AI JSON field is too long: " + field);
        }
        return text;
    }

    private List<String> requiredTextArray(JsonNode node, String field, int maxItems, int maxLength) {
        JsonNode value = node.get(field);
        if (value == null || !value.isArray()) {
            throw new IllegalArgumentException("AI JSON item is missing array field: " + field);
        }
        if (value.size() > maxItems) {
            throw new IllegalArgumentException("AI JSON array has too many values: " + field);
        }

        List<String> values = new ArrayList<>();
        for (JsonNode entry : value) {
            if (!entry.isTextual()) {
                throw new IllegalArgumentException("AI JSON array must contain strings: " + field);
            }
            String text = entry.asText().trim();
            if (!text.isEmpty()) {
                if (text.length() > maxLength) {
                    throw new IllegalArgumentException("AI JSON array value is too long: " + field);
                }
                values.add(text);
            }
        }
        return values;
    }
}
