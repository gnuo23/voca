package com.voca.backend.review;

import com.voca.backend.vocab.UserProgress;
import com.voca.backend.vocab.VocabProgressStatus;

import java.time.LocalDateTime;

public record ReviewProgressResponse(
        Long vocabId,
        VocabProgressStatus status,
        ReviewQuality quality,
        int correctCount,
        int wrongCount,
        int streakCorrectCount,
        double easeFactor,
        int intervalDays,
        int repetitionCount,
        int lapseCount,
        LocalDateTime lastReviewedAt,
        LocalDateTime nextReviewAt
) {
    static ReviewProgressResponse from(UserProgress progress, ReviewQuality quality) {
        return new ReviewProgressResponse(
                progress.getVocabItem().getId(),
                progress.getStatus(),
                quality,
                progress.getCorrectCount(),
                progress.getWrongCount(),
                progress.getStreakCorrectCount(),
                progress.getEaseFactor(),
                progress.getIntervalDays(),
                progress.getRepetitionCount(),
                progress.getLapseCount(),
                progress.getLastReviewedAt(),
                progress.getNextReviewAt()
        );
    }
}
