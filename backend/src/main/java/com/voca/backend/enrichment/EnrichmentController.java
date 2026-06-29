package com.voca.backend.enrichment;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class EnrichmentController {

    private final EnrichmentService enrichmentService;

    public EnrichmentController(EnrichmentService enrichmentService) {
        this.enrichmentService = enrichmentService;
    }

    @PostMapping("/vocab/{vocabId}/enrich")
    public EnrichmentJobResponse enrichVocab(Authentication authentication, @PathVariable Long vocabId) {
        return enrichmentService.enrichVocab(authentication, vocabId);
    }

    @PostMapping("/decks/{deckId}/enrich")
    public EnrichmentJobResponse enrichDeck(Authentication authentication, @PathVariable Long deckId) {
        return enrichmentService.enrichDeck(authentication, deckId);
    }

    @GetMapping("/enrich/jobs/{jobId}")
    public EnrichmentJobResponse getJob(Authentication authentication, @PathVariable Long jobId) {
        return enrichmentService.getJob(authentication, jobId);
    }
}
