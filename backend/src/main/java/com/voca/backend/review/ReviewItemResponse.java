package com.voca.backend.review;

import com.voca.backend.vocab.UserProgress;
import com.voca.backend.vocab.VocabItem;
import com.voca.backend.vocab.VocabProgressStatus;

import java.time.LocalDateTime;

public record ReviewItemResponse(
        Long vocabId,
        Long deckId,
        String word,
        String partOfSpeech,
        String meaningVi,
        String ipaUs,
        String exampleEn,
        String exampleVi,
        VocabProgressStatus status,
        LocalDateTime nextReviewAt,
        int wrongCount,
        int correctCount,
        int lapseCount
) {
    static ReviewItemResponse from(VocabItem item, UserProgress progress) {
        return new ReviewItemResponse(
                item.getId(),
                item.getDeck().getId(),
                item.getWord(),
                item.getPartOfSpeech(),
                item.getMeaningVi(),
                item.getIpa(),
                item.getExampleEn(),
                item.getExampleVi(),
                progress == null ? VocabProgressStatus.NEW : progress.getStatus(),
                progress == null ? null : progress.getNextReviewAt(),
                progress == null ? 0 : progress.getWrongCount(),
                progress == null ? 0 : progress.getCorrectCount(),
                progress == null ? 0 : progress.getLapseCount()
        );
    }
}
