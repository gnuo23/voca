package com.voca.backend.quiz;

import java.time.LocalDateTime;
import java.util.List;

public record QuizResultResponse(
        Long attemptId,
        Long deckId,
        int totalQuestions,
        int answeredCount,
        int correctCount,
        int scorePercent,
        boolean completed,
        LocalDateTime completedAt,
        List<QuizAnswerResponse> answers
) {
}
