package com.voca.backend.enrichment;

public record EnrichmentVocabInput(
        Long id,
        String word,
        String partOfSpeech,
        String meaningVi
) {
}
