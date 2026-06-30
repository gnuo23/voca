package com.voca.backend.review;

import com.voca.backend.vocab.UserProgress;
import com.voca.backend.vocab.VocabProgressStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ReviewSchedulingService {

    private static final int AGAIN_REVIEW_MINUTES = 10;
    private static final int GOOD_FIRST_REVIEW_DAYS = 1;
    private static final int GOOD_SECOND_REVIEW_DAYS = 3;
    private static final int GOOD_THIRD_REVIEW_DAYS = 7;
    private static final int EASY_FIRST_REVIEW_DAYS = 4;
    private static final int MASTERED_INTERVAL_DAYS = 21;

    public UserProgress apply(UserProgress progress, ReviewQuality quality, Integer responseTimeMs, LocalDateTime now) {
        switch (quality) {
            case AGAIN -> applyAgain(progress, now);
            case HARD -> applyHard(progress, now);
            case GOOD -> applyGood(progress, now);
            case EASY -> applyEasy(progress, now);
        }

        progress.setLastQuality(quality.name());
        progress.setLastResponseTimeMs(responseTimeMs);
        progress.setLastReviewedAt(now);
        progress.setLastMarkedAt(now);
        return progress;
    }

    public ReviewQuality infer(Boolean isCorrect, Integer responseTimeMs) {
        if (Boolean.FALSE.equals(isCorrect)) {
            return ReviewQuality.AGAIN;
        }
        if (!Boolean.TRUE.equals(isCorrect)) {
            return ReviewQuality.GOOD;
        }
        if (responseTimeMs != null && responseTimeMs <= 3000) {
            return ReviewQuality.EASY;
        }
        if (responseTimeMs != null && responseTimeMs <= 8000) {
            return ReviewQuality.GOOD;
        }
        return ReviewQuality.HARD;
    }

    private void applyAgain(UserProgress progress, LocalDateTime now) {
        progress.setRepetitionCount(0);
        progress.setStreakCorrectCount(0);
        progress.setWrongCount(progress.getWrongCount() + 1);
        progress.setLapseCount(progress.getLapseCount() + 1);
        progress.setEaseFactor(Math.max(1.3, progress.getEaseFactor() - 0.2));
        progress.setIntervalDays(0);
        progress.setNextReviewAt(now.plusMinutes(AGAIN_REVIEW_MINUTES));
        progress.setStatus(progress.getLapseCount() >= 4 ? VocabProgressStatus.DIFFICULT : VocabProgressStatus.LEARNING);
        progress.incrementUnknownCount();
    }

    private void applyHard(UserProgress progress, LocalDateTime now) {
        progress.setCorrectCount(progress.getCorrectCount() + 1);
        progress.setStreakCorrectCount(progress.getStreakCorrectCount() + 1);
        progress.setRepetitionCount(progress.getRepetitionCount() + 1);
        progress.setEaseFactor(Math.max(1.3, progress.getEaseFactor() - 0.15));
        int baseInterval = Math.max(1, progress.getIntervalDays());
        int nextInterval = Math.max(1, (int) Math.round(baseInterval * 0.5));
        progress.setIntervalDays(nextInterval);
        progress.setNextReviewAt(now.plusDays(nextInterval));
        progress.setStatus(VocabProgressStatus.REVIEW);
        progress.incrementDifficultCount();
    }

    private void applyGood(UserProgress progress, LocalDateTime now) {
        progress.setCorrectCount(progress.getCorrectCount() + 1);
        progress.setStreakCorrectCount(progress.getStreakCorrectCount() + 1);
        progress.setRepetitionCount(progress.getRepetitionCount() + 1);

        int nextInterval;
        if (progress.getRepetitionCount() == 1) {
            progress.setIntervalDays(GOOD_FIRST_REVIEW_DAYS);
            progress.setNextReviewAt(now.plusDays(GOOD_FIRST_REVIEW_DAYS));
            progress.setStatus(VocabProgressStatus.REVIEW);
            progress.incrementKnownCount();
            return;
        } else if (progress.getRepetitionCount() == 2) {
            progress.setIntervalDays(GOOD_SECOND_REVIEW_DAYS);
            progress.setNextReviewAt(now.plusDays(GOOD_SECOND_REVIEW_DAYS));
            progress.setStatus(VocabProgressStatus.REVIEW);
            progress.incrementKnownCount();
            return;
        } else if (progress.getRepetitionCount() == 3) {
            nextInterval = GOOD_THIRD_REVIEW_DAYS;
        } else {
            double multiplier = Math.max(1.5, progress.getEaseFactor() * 0.75);
            nextInterval = Math.max(1, (int) Math.round(Math.max(1, progress.getIntervalDays()) * multiplier));
        }

        progress.setIntervalDays(nextInterval);
        progress.setNextReviewAt(now.plusDays(nextInterval));
        progress.setStatus(nextInterval >= MASTERED_INTERVAL_DAYS ? VocabProgressStatus.MASTERED : VocabProgressStatus.REVIEW);
        progress.incrementKnownCount();
    }

    private void applyEasy(UserProgress progress, LocalDateTime now) {
        progress.setCorrectCount(progress.getCorrectCount() + 1);
        progress.setStreakCorrectCount(progress.getStreakCorrectCount() + 1);
        progress.setRepetitionCount(progress.getRepetitionCount() + 1);
        progress.setEaseFactor(Math.min(3.0, progress.getEaseFactor() + 0.15));
        int nextInterval = progress.getIntervalDays() <= 0
                ? EASY_FIRST_REVIEW_DAYS
                : Math.max(1, (int) Math.round(progress.getIntervalDays() * Math.min(2.0, progress.getEaseFactor())));
        progress.setIntervalDays(nextInterval);
        progress.setNextReviewAt(now.plusDays(nextInterval));
        progress.setStatus(nextInterval >= MASTERED_INTERVAL_DAYS ? VocabProgressStatus.MASTERED : VocabProgressStatus.REVIEW);
        progress.incrementKnownCount();
    }
}
