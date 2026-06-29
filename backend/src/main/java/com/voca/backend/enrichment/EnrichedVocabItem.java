package com.voca.backend.enrichment;

import java.util.List;

public record EnrichedVocabItem(
        Long id,
        String ipa,
        String pronunciationHint,
        String exampleEn,
        String exampleVi,
        String topic,
        String level,
        List<String> synonyms,
        List<String> antonyms,
        List<String> collocations
) {
}
