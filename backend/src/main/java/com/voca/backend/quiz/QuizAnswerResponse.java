package com.voca.backend.quiz;

import java.time.LocalDateTime;

public record QuizAnswerResponse(
        Long questionId,
        String answer,
        String correctAnswer,
        boolean correct,
        String explanation,
        LocalDateTime answeredAt
) {
}
