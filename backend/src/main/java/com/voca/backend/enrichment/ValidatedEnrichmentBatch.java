package com.voca.backend.enrichment;

import java.util.List;

public record ValidatedEnrichmentBatch(
        List<EnrichedVocabItem> items
) {
}
