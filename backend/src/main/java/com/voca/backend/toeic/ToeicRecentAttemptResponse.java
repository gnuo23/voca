package com.voca.backend.toeic;

import java.time.LocalDateTime;

public record ToeicRecentAttemptResponse(
        Long attemptId,
        String testSlug,
        String testName,
        String mode,
        String partFilter,
        int totalQuestions,
        int correctCount,
        Integer scaledScore,
        LocalDateTime completedAt
) {
}
