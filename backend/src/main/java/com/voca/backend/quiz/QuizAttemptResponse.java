package com.voca.backend.quiz;

import java.time.LocalDateTime;
import java.util.List;

public record QuizAttemptResponse(
        Long id,
        Long deckId,
        int totalQuestions,
        int answeredCount,
        int correctCount,
        boolean completed,
        LocalDateTime completedAt,
        LocalDateTime createdAt,
        List<QuizQuestionResponse> questions
) {
}
