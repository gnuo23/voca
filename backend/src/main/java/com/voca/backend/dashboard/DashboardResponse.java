package com.voca.backend.dashboard;

import java.util.List;

public record DashboardResponse(
        long wordsLearnedToday,
        long wordsReviewedToday,
        long wordsToReview,
        long overdueWords,
        double accuracy,
        int streakDays,
        boolean streakActiveToday,
        List<StreakDayResponse> streakWeek,
        List<HardWordResponse> hardWords,
        List<DeckProgressResponse> deckProgress
) {
}
