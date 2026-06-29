package com.voca.backend.enrichment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.voca.backend.vocab.VocabItem;
import com.voca.backend.vocab.VocabItemRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class EnrichmentWorker {

    private final EnrichmentJobRepository enrichmentJobRepository;
    private final VocabItemRepository vocabItemRepository;
    private final VocabularyEnrichmentClient enrichmentClient;
    private final EnrichmentJsonValidator jsonValidator;
    private final EnrichmentProperties properties;
    private final ObjectMapper objectMapper;

    public EnrichmentWorker(
            EnrichmentJobRepository enrichmentJobRepository,
            VocabItemRepository vocabItemRepository,
            VocabularyEnrichmentClient enrichmentClient,
            EnrichmentJsonValidator jsonValidator,
            EnrichmentProperties properties,
            ObjectMapper objectMapper
    ) {
        this.enrichmentJobRepository = enrichmentJobRepository;
        this.vocabItemRepository = vocabItemRepository;
        this.enrichmentClient = enrichmentClient;
        this.jsonValidator = jsonValidator;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Async
    public void process(Long jobId) {
        EnrichmentJob job = enrichmentJobRepository.findById(jobId).orElseThrow();
        job.setStatus(EnrichmentJobStatus.PROCESSING);
        enrichmentJobRepository.save(job);

        try {
            List<VocabItem> items = loadItems(job);
            for (List<VocabItem> batch : batches(items, properties.batchSize())) {
                enrichBatch(job, batch);
            }

            job.setStatus(EnrichmentJobStatus.DONE);
            job.setCompletedAt(LocalDateTime.now());
            enrichmentJobRepository.save(job);
        } catch (Exception ex) {
            job.setStatus(EnrichmentJobStatus.FAILED);
            job.setErrorMessage(trim(ex.getMessage(), 1000));
            job.setCompletedAt(LocalDateTime.now());
            enrichmentJobRepository.save(job);
        }
    }

    private List<VocabItem> loadItems(EnrichmentJob job) {
        if (job.getVocabItem() != null) {
            return vocabItemRepository.findByIdAndDeckOwnerId(job.getVocabItem().getId(), job.getOwner().getId())
                    .filter(this::hasMissingDetails)
                    .map(List::of)
                    .orElse(List.of());
        }

        if (job.getDeck() == null) {
            return List.of();
        }

        return vocabItemRepository
                .findAllByDeckIdAndDeckOwnerIdOrderByCreatedAtAsc(job.getDeck().getId(), job.getOwner().getId())
                .stream()
                .filter(this::hasMissingDetails)
                .toList();
    }

    private void enrichBatch(EnrichmentJob job, List<VocabItem> batch) {
        if (batch.isEmpty()) {
            return;
        }

        List<EnrichmentVocabInput> inputs = batch.stream()
                .map(item -> new EnrichmentVocabInput(
                        item.getId(),
                        item.getWord(),
                        item.getPartOfSpeech(),
                        item.getMeaningVi()
                ))
                .toList();
        List<Long> expectedIds = inputs.stream().map(EnrichmentVocabInput::id).toList();

        ValidatedEnrichmentBatch enrichmentBatch = retry(() -> {
            String rawJson = enrichmentClient.enrichBatch(inputs);
            return jsonValidator.validate(rawJson, expectedIds);
        });

        Map<Long, VocabItem> itemsById = batch.stream()
                .collect(Collectors.toMap(VocabItem::getId, Function.identity()));
        for (EnrichedVocabItem enriched : enrichmentBatch.items()) {
            applyMissingFields(itemsById.get(enriched.id()), enriched);
        }

        vocabItemRepository.saveAll(batch);
        job.setProcessedItems(job.getProcessedItems() + batch.size());
        enrichmentJobRepository.save(job);
    }

    private ValidatedEnrichmentBatch retry(EnrichmentCall call) {
        RuntimeException last = null;
        for (int attempt = 1; attempt <= properties.maxRetries(); attempt++) {
            try {
                return call.run();
            } catch (RuntimeException ex) {
                last = ex;
            }
        }
        throw last == null ? new IllegalStateException("AI enrichment failed") : last;
    }

    private void applyMissingFields(VocabItem item, EnrichedVocabItem enriched) {
        if (item == null) {
            return;
        }

        if (isBlank(item.getIpa())) {
            item.setIpa(enriched.ipa());
        }
        if (isBlank(item.getPronunciationHint())) {
            item.setPronunciationHint(enriched.pronunciationHint());
        }
        if (isBlank(item.getExampleEn())) {
            item.setExampleEn(enriched.exampleEn());
        }
        if (isBlank(item.getExampleVi())) {
            item.setExampleVi(enriched.exampleVi());
        }
        if (isBlank(item.getTopic())) {
            item.setTopic(enriched.topic());
        }
        if (isBlank(item.getLevel())) {
            item.setLevel(enriched.level());
        }
        if (isBlank(item.getSynonyms())) {
            item.setSynonyms(toJson(enriched.synonyms()));
        }
        if (isBlank(item.getAntonyms())) {
            item.setAntonyms(toJson(enriched.antonyms()));
        }
        if (isBlank(item.getCollocations())) {
            item.setCollocations(toJson(enriched.collocations()));
        }
        item.setEnrichedAt(LocalDateTime.now());
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

    private List<List<VocabItem>> batches(List<VocabItem> items, int batchSize) {
        List<List<VocabItem>> batches = new ArrayList<>();
        for (int start = 0; start < items.size(); start += batchSize) {
            batches.add(items.subList(start, Math.min(start + batchSize, items.size())));
        }
        return batches;
    }

    private String toJson(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values == null ? List.of() : values);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not serialize enrichment array", ex);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String trim(String value, int maxLength) {
        if (value == null) {
            return "AI enrichment failed";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    @FunctionalInterface
    private interface EnrichmentCall {
        ValidatedEnrichmentBatch run();
    }
}
