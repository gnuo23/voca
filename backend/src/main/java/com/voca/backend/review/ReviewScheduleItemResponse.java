package com.voca.backend.review;

import com.voca.backend.vocab.UserProgress;
import com.voca.backend.vocab.VocabItem;
import com.voca.backend.vocab.VocabProgressStatus;

import java.time.Duration;
import java.time.LocalDateTime;

public record ReviewScheduleItemResponse(
        Long vocabId,
        Long deckId,
        String deckName,
        String word,
        String partOfSpeech,
        String meaningVi,
        VocabProgressStatus status,
        ReviewScheduleBucket bucket,
        LocalDateTime lastReviewedAt,
        LocalDateTime nextReviewAt,
        Long minutesUntilReview,
        int correctCount,
        int wrongCount,
        int lapseCount,
        int repetitionCount
) {
    static ReviewScheduleItemResponse from(VocabItem item, UserProgress progress, LocalDateTime now) {
        LocalDateTime nextReviewAt = progress == null ? null : progress.getNextReviewAt();
        return new ReviewScheduleItemResponse(
                item.getId(),
                item.getDeck().getId(),
                item.getDeck().getName(),
                item.getWord(),
                item.getPartOfSpeech(),
                item.getMeaningVi(),
                progress == null ? VocabProgressStatus.NEW : progress.getStatus(),
                ReviewService.scheduleBucket(progress, now),
                progress == null ? null : progress.getLastReviewedAt(),
                nextReviewAt,
                nextReviewAt == null ? 0 : Duration.between(now, nextReviewAt).toMinutes(),
                progress == null ? 0 : progress.getCorrectCount(),
                progress == null ? 0 : progress.getWrongCount(),
                progress == null ? 0 : progress.getLapseCount(),
                progress == null ? 0 : progress.getRepetitionCount()
        );
    }
}
