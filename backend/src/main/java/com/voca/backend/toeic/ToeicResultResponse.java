package com.voca.backend.toeic;

import java.time.LocalDateTime;
import java.util.List;

public record ToeicResultResponse(
        Long attemptId,
        Long testId,
        String testSlug,
        String testName,
        String mode,
        String partFilter,
        int totalQuestions,
        int answeredCount,
        int correctCount,
        int listeningCorrect,
        int readingCorrect,
        Integer listeningScore,
        Integer readingScore,
        Integer scaledScore,
        boolean completed,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        List<ToeicPartBreakdownResponse> partBreakdown,
        List<ToeicResultAnswerResponse> answers
) {
}
