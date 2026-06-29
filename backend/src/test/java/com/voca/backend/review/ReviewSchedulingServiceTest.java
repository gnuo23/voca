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
    void againSchedulesNextReviewInThreeMinutes() {
        UserProgress progress = progress();

        service.apply(progress, ReviewQuality.AGAIN, 9000, now);

        assertThat(progress.getNextReviewAt()).isEqualTo(now.plusMinutes(3));
        assertThat(progress.getStatus()).isEqualTo(VocabProgressStatus.LEARNING);
    }

    @Test
    void againReducesEaseFactorButNotBelowMinimum() {
        UserProgress progress = progress();
        progress.setEaseFactor(1.35);

        service.apply(progress, ReviewQuality.AGAIN, 9000, now);

        assertThat(progress.getEaseFactor()).isEqualTo(1.3);
    }

    @Test
    void hardReducesEaseFactorAndSchedulesInThirtyMinutes() {
        UserProgress progress = progress();

        service.apply(progress, ReviewQuality.HARD, 9000, now);

        assertThat(progress.getEaseFactor()).isEqualTo(2.35);
        assertThat(progress.getIntervalDays()).isEqualTo(0);
        assertThat(progress.getNextReviewAt()).isEqualTo(now.plusMinutes(30));
    }

    @Test
    void goodFirstRepetitionSchedulesFourHours() {
        UserProgress progress = progress();

        service.apply(progress, ReviewQuality.GOOD, 4200, now);

        assertThat(progress.getIntervalDays()).isEqualTo(0);
        assertThat(progress.getNextReviewAt()).isEqualTo(now.plusHours(4));
    }

    @Test
    void goodSecondRepetitionSchedulesTwelveHours() {
        UserProgress progress = progress();
        progress.setRepetitionCount(1);

        service.apply(progress, ReviewQuality.GOOD, 4200, now);

        assertThat(progress.getIntervalDays()).isEqualTo(0);
        assertThat(progress.getNextReviewAt()).isEqualTo(now.plusHours(12));
    }

    @Test
    void goodThirdRepetitionSchedulesOneDay() {
        UserProgress progress = progress();
        progress.setRepetitionCount(2);

        service.apply(progress, ReviewQuality.GOOD, 4200, now);

        assertThat(progress.getIntervalDays()).isEqualTo(1);
        assertThat(progress.getNextReviewAt()).isEqualTo(now.plusDays(1));
    }

    @Test
    void goodLaterRepetitionsUseShorterEaseMultiplier() {
        UserProgress progress = progress();
        progress.setRepetitionCount(3);
        progress.setIntervalDays(1);
        progress.setEaseFactor(2.5);

        service.apply(progress, ReviewQuality.GOOD, 4200, now);

        assertThat(progress.getIntervalDays()).isEqualTo(2);
    }

    @Test
    void easyIncreasesEaseFactorButNotAboveMaximum() {
        UserProgress progress = progress();
        progress.setEaseFactor(2.95);

        service.apply(progress, ReviewQuality.EASY, 1000, now);

        assertThat(progress.getEaseFactor()).isEqualTo(3.0);
    }

    @Test
    void easySetsMasteredIfIntervalAtLeastFourteenDays() {
        UserProgress progress = progress();
        progress.setIntervalDays(7);
        progress.setEaseFactor(2.5);

        service.apply(progress, ReviewQuality.EASY, 1000, now);

        assertThat(progress.getIntervalDays()).isGreaterThanOrEqualTo(14);
        assertThat(progress.getStatus()).isEqualTo(VocabProgressStatus.MASTERED);
    }

    @Test
    void repeatedLapsesSetStatusDifficult() {
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
