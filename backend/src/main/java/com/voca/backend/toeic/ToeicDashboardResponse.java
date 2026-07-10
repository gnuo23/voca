package com.voca.backend.toeic;

import java.util.List;

public record ToeicDashboardResponse(
        int accuracyPercent,
        int streakDays,
        long completedAttempts,
        long totalAnswered,
        long totalCorrect,
        long answeredToday,
        long completedToday,
        long incorrectToday,
        List<ToeicPartProgressResponse> partProgress,
        List<ToeicRecentAttemptResponse> recentAttempts
) {
}
