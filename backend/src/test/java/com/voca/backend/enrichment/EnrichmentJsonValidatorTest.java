package com.voca.backend.enrichment;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EnrichmentJsonValidatorTest {

    private final EnrichmentJsonValidator validator = new EnrichmentJsonValidator(new ObjectMapper());

    @Test
    void validatesStrictEnrichmentJson() {
        ValidatedEnrichmentBatch batch = validator.validate("""
                {
                  "items": [
                    {
                      "id": 1,
                      "ipa": "/ˈæbsənt/",
                      "pronunciationHint": "AB-suhnt",
                      "exampleEn": "She was absent from class.",
                      "exampleVi": "Co ay vang mat trong lop.",
                      "topic": "School",
                      "level": "ELEMENTARY",
                      "synonyms": ["away"],
                      "antonyms": ["present"],
                      "collocations": ["absent from class"]
                    }
                  ]
                }
                """, List.of(1L));

        assertThat(batch.items()).hasSize(1);
        assertThat(batch.items().getFirst().ipa()).isEqualTo("/ˈæbsənt/");
    }

    @Test
    void rejectsInvalidJsonShape() {
        assertThatThrownBy(() -> validator.validate("""
                {"items":[{"id":1,"ipa":"/x/","level":"BAD"}]}
                """, List.of(1L)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
