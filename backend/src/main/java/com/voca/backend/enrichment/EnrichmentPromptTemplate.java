package com.voca.backend.enrichment;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EnrichmentPromptTemplate {

    public String build(List<EnrichmentVocabInput> items) {
        StringBuilder builder = new StringBuilder();
        builder.append("""
                You enrich English vocabulary for Vietnamese learners.
                Return strict JSON only, no markdown, no comments.
                Schema:
                {
                  "items": [
                    {
                      "id": number,
                      "ipa": string,
                      "pronunciationHint": string,
                      "exampleEn": string,
                      "exampleVi": string,
                      "topic": string,
                      "level": "BEGINNER|ELEMENTARY|INTERMEDIATE|UPPER_INTERMEDIATE|ADVANCED",
                      "synonyms": string[],
                      "antonyms": string[],
                      "collocations": string[]
                    }
                  ]
                }
                Do not rewrite or translate the user's meaning. Only add missing enrichment fields.
                Keep examples natural, short, and suitable for learners.
                Items:
                """);

        for (EnrichmentVocabInput item : items) {
            builder
                    .append("- id: ").append(item.id())
                    .append(", word: ").append(item.word())
                    .append(", pos: ").append(nullToBlank(item.partOfSpeech()))
                    .append(", meaningVi: ").append(nullToBlank(item.meaningVi()))
                    .append("\n");
        }

        return builder.toString();
    }

    private String nullToBlank(String value) {
        return value == null ? "" : value;
    }
}
