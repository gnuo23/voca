package com.voca.backend.dashboard;

import java.util.List;

public record DashboardResponse(
        long wordsLearnedToday,
        long wordsReviewedToday,
        long wordsToReview,
        long overdueWords,
        double accuracy,
        LearningLevelResponse level,
        int streakDays,
        boolean streakActiveToday,
        List<StreakDayResponse> streakWeek,
        List<WeeklyStatResponse> weeklyStats,
        List<RecentActivityResponse> recentActivities,
        List<HardWordResponse> hardWords,
        List<DeckProgressResponse> deckProgress
) {
}
