package com.voca.backend.review;

import com.voca.backend.vocab.UserProgress;
import com.voca.backend.vocab.VocabProgressStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ReviewSchedulingServiceTest {

    private final ReviewSchedulingService service = new ReviewSchedulingService();
    private final LocalDateTime now = LocalDateTime.of(2026, 6, 29, 20, 15);

    @Test
    void againSchedulesNextReviewInThirtyMinutes() {
        UserProgress progress = progress();

        service.apply(progress, ReviewQuality.AGAIN, 9000, now);

        assertThat(progress.getNextReviewAt()).isEqualTo(now.plusMinutes(30));
        assertThat(progress.getStatus()).isEqualTo(VocabProgressStatus.DIFFICULT);
    }

    @Test
    void againReducesEaseFactorButNotBelowMinimum() {
        UserProgress progress = progress();
        progress.setEaseFactor(1.35);

        service.apply(progress, ReviewQuality.AGAIN, 9000, now);

        assertThat(progress.getEaseFactor()).isEqualTo(1.3);
    }

    @Test
    void hardReducesEaseFactorAndSchedulesInTwoDaysForNearReviews() {
        UserProgress progress = progress();

        service.apply(progress, ReviewQuality.HARD, 9000, now);

        assertThat(progress.getEaseFactor()).isEqualTo(2.35);
        assertThat(progress.getIntervalDays()).isEqualTo(2);
        assertThat(progress.getNextReviewAt()).isEqualTo(now.plusDays(2));
        assertThat(progress.getStatus()).isEqualTo(VocabProgressStatus.REVIEW);
    }

    @Test
    void hardKeepsFarReviewHalvingRule() {
        UserProgress progress = progress();
        progress.setIntervalDays(20);

        service.apply(progress, ReviewQuality.HARD, 9000, now);

        assertThat(progress.getIntervalDays()).isEqualTo(10);
        assertThat(progress.getNextReviewAt()).isEqualTo(now.plusDays(10));
    }

    @Test
    void goodFirstRepetitionSchedulesTwoDays() {
        UserProgress progress = progress();

        service.apply(progress, ReviewQuality.GOOD, 4200, now);

        assertThat(progress.getIntervalDays()).isEqualTo(2);
        assertThat(progress.getNextReviewAt()).isEqualTo(now.plusDays(2));
    }

    @Test
    void goodReviewMovesDifficultWordBackToReview() {
        UserProgress progress = progress();
        progress.setStatus(VocabProgressStatus.DIFFICULT);

        service.apply(progress, ReviewQuality.GOOD, 4200, now);

        assertThat(progress.getStatus()).isEqualTo(VocabProgressStatus.REVIEW);
    }

    @Test
    void goodSecondRepetitionSchedulesFourDays() {
        UserProgress progress = progress();
        progress.setRepetitionCount(1);

        service.apply(progress, ReviewQuality.GOOD, 4200, now);

        assertThat(progress.getIntervalDays()).isEqualTo(4);
        assertThat(progress.getNextReviewAt()).isEqualTo(now.plusDays(4));
    }

    @Test
    void goodThirdRepetitionSchedulesSevenDays() {
        UserProgress progress = progress();
        progress.setRepetitionCount(2);

        service.apply(progress, ReviewQuality.GOOD, 4200, now);

        assertThat(progress.getIntervalDays()).isEqualTo(7);
        assertThat(progress.getNextReviewAt()).isEqualTo(now.plusDays(7));
    }

    @Test
    void goodLaterRepetitionsUseShorterEaseMultiplier() {
        UserProgress progress = progress();
        progress.setRepetitionCount(3);
        progress.setIntervalDays(7);
        progress.setEaseFactor(2.5);

        service.apply(progress, ReviewQuality.GOOD, 4200, now);

        assertThat(progress.getIntervalDays()).isEqualTo(13);
    }

    @Test
    void easyIncreasesEaseFactorButNotAboveMaximum() {
        UserProgress progress = progress();
        progress.setEaseFactor(2.95);

        service.apply(progress, ReviewQuality.EASY, 1000, now);

        assertThat(progress.getEaseFactor()).isEqualTo(3.0);
    }

    @Test
    void easyFirstRepetitionSchedulesFiveDays() {
        UserProgress progress = progress();

        service.apply(progress, ReviewQuality.EASY, 1000, now);

        assertThat(progress.getIntervalDays()).isEqualTo(5);
        assertThat(progress.getNextReviewAt()).isEqualTo(now.plusDays(5));
    }

    @Test
    void easySetsMasteredIfIntervalAtLeastTwentyOneDays() {
        UserProgress progress = progress();
        progress.setIntervalDays(11);
        progress.setEaseFactor(2.5);

        service.apply(progress, ReviewQuality.EASY, 1000, now);

        assertThat(progress.getIntervalDays()).isGreaterThanOrEqualTo(21);
        assertThat(progress.getStatus()).isEqualTo(VocabProgressStatus.MASTERED);
    }

    @Test
    void fourthLapseSetsStatusDifficult() {
        UserProgress progress = progress();
        progress.setLapseCount(3);

        service.apply(progress, ReviewQuality.AGAIN, 9000, now);

        assertThat(progress.getLapseCount()).isEqualTo(4);
        assertThat(progress.getStatus()).isEqualTo(VocabProgressStatus.DIFFICULT);
    }

    @Test
    void thirdLapseSetsStatusDifficult() {
        UserProgress progress = progress();
        progress.setLapseCount(2);

        service.apply(progress, ReviewQuality.AGAIN, 9000, now);

        assertThat(progress.getLapseCount()).isEqualTo(3);
        assertThat(progress.getStatus()).isEqualTo(VocabProgressStatus.DIFFICULT);
    }

    private UserProgress progress() {
        UserProgress progress = new UserProgress();
        progress.setEaseFactor(2.5);
        return progress;
    }
}
