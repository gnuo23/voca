package com.voca.backend.vocab;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;

public record VocabItemResponse(
        Long id,
        Long deckId,
        String word,
        String partOfSpeech,
        String meaningVi,
        String ipa,
        String pronunciationHint,
        String exampleEn,
        String exampleVi,
        String topic,
        String level,
        List<String> synonyms,
        List<String> antonyms,
        List<String> collocations,
        LocalDateTime enrichedAt,
        String audioUrl,
        String audioUsUrl,
        String audioUkUrl,
        String audioAccent,
        String audioSource,
        LocalDateTime audioRefreshedAt,
        VocabProgressStatus progressStatus,
        int knownCount,
        int unknownCount,
        int difficultCount,
        LocalDateTime lastMarkedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    static VocabItemResponse from(VocabItem item, UserProgress progress) {
        return new VocabItemResponse(
                item.getId(),
                item.getDeck().getId(),
                item.getWord(),
                item.getPartOfSpeech(),
                item.getMeaningVi(),
                item.getIpa(),
                item.getPronunciationHint(),
                item.getExampleEn(),
                item.getExampleVi(),
                item.getTopic(),
                item.getLevel(),
                parseList(item.getSynonyms()),
                parseList(item.getAntonyms()),
                parseList(item.getCollocations()),
                item.getEnrichedAt(),
                item.getAudioUrl(),
                item.getAudioUsUrl(),
                item.getAudioUkUrl(),
                item.getAudioAccent(),
                item.getAudioSource(),
                item.getAudioRefreshedAt(),
                progress == null ? VocabProgressStatus.NEW : progress.getStatus(),
                progress == null ? 0 : progress.getKnownCount(),
                progress == null ? 0 : progress.getUnknownCount(),
                progress == null ? 0 : progress.getDifficultCount(),
                progress == null ? null : progress.getLastMarkedAt(),
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }

    private static List<String> parseList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }

        try {
            return new ObjectMapper().readValue(json, new TypeReference<>() {
            });
        } catch (JsonProcessingException ex) {
            return List.of();
        }
    }
}
