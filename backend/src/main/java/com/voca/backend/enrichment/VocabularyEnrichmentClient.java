package com.voca.backend.enrichment;

import java.util.List;

public interface VocabularyEnrichmentClient {

    String enrichBatch(List<EnrichmentVocabInput> items);
}
