package com.voca.backend.enrichment;

import com.voca.backend.deck.Deck;
import com.voca.backend.deck.DeckService;
import com.voca.backend.user.User;
import com.voca.backend.user.UserService;
import com.voca.backend.vocab.VocabItem;
import com.voca.backend.vocab.VocabItemRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EnrichmentService {

    private final EnrichmentJobRepository enrichmentJobRepository;
    private final EnrichmentWorker enrichmentWorker;
    private final VocabItemRepository vocabItemRepository;
    private final DeckService deckService;
    private final UserService userService;

    public EnrichmentService(
            EnrichmentJobRepository enrichmentJobRepository,
            EnrichmentWorker enrichmentWorker,
            VocabItemRepository vocabItemRepository,
            DeckService deckService,
            UserService userService
    ) {
        this.enrichmentJobRepository = enrichmentJobRepository;
        this.enrichmentWorker = enrichmentWorker;
        this.vocabItemRepository = vocabItemRepository;
        this.deckService = deckService;
        this.userService = userService;
    }

    @Transactional
    public EnrichmentJobResponse enrichVocab(Authentication authentication, Long vocabId) {
        User owner = userService.currentUser(authentication);
        VocabItem item = vocabItemRepository.findByIdAndDeckOwnerId(vocabId, owner.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vocabulary item not found"));

        EnrichmentJob job = new EnrichmentJob();
        job.setOwner(owner);
        job.setDeck(item.getDeck());
        job.setVocabItem(item);
        job.setTotalItems(hasMissingDetails(item) ? 1 : 0);
        job = enrichmentJobRepository.save(job);

        startOrComplete(job);
        return EnrichmentJobResponse.from(job);
    }

    @Transactional
    public EnrichmentJobResponse enrichDeck(Authentication authentication, Long deckId) {
        User owner = userService.currentUser(authentication);
        Deck deck = deckService.findOwnedDeck(owner, deckId);
        List<VocabItem> items = vocabItemRepository
                .findAllByDeckIdAndDeckOwnerIdOrderByCreatedAtAsc(deck.getId(), owner.getId())
                .stream()
                .filter(this::hasMissingDetails)
                .toList();

        EnrichmentJob job = new EnrichmentJob();
        job.setOwner(owner);
        job.setDeck(deck);
        job.setTotalItems(items.size());
        job = enrichmentJobRepository.save(job);

        startOrComplete(job);
        return EnrichmentJobResponse.from(job);
    }

    @Transactional(readOnly = true)
    public EnrichmentJobResponse getJob(Authentication authentication, Long jobId) {
        User owner = userService.currentUser(authentication);
        EnrichmentJob job = enrichmentJobRepository.findByIdAndOwnerId(jobId, owner.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Enrichment job not found"));
        return EnrichmentJobResponse.from(job);
    }

    private void startOrComplete(EnrichmentJob job) {
        if (job.getTotalItems() == 0) {
            job.setStatus(EnrichmentJobStatus.DONE);
            job.setCompletedAt(LocalDateTime.now());
            enrichmentJobRepository.save(job);
            return;
        }

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            Long jobId = job.getId();
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    enrichmentWorker.process(jobId);
                }
            });
            return;
        }

        enrichmentWorker.process(job.getId());
    }

    private boolean hasMissingDetails(VocabItem item) {
        return isBlank(item.getIpa())
                || isBlank(item.getPronunciationHint())
                || isBlank(item.getExampleEn())
                || isBlank(item.getExampleVi())
                || isBlank(item.getTopic())
                || isBlank(item.getLevel())
                || isBlank(item.getSynonyms())
                || isBlank(item.getAntonyms())
                || isBlank(item.getCollocations());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
