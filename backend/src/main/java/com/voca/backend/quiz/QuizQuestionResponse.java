package com.voca.backend.quiz;

import java.time.LocalDateTime;
public record QuizQuestionResponse(
        Long id,
        Long deckId,
        Long vocabId,
        QuestionType type,
        String prompt,
        Object options,
        String explanation,
        LocalDateTime createdAt
) {
}
