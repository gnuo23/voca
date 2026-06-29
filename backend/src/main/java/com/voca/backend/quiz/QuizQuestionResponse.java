package com.voca.backend.quiz;

import java.time.LocalDateTime;
import java.util.List;

public record QuizQuestionResponse(
        Long id,
        Long deckId,
        Long vocabId,
        QuestionType type,
        String prompt,
        List<String> options,
        String explanation,
        LocalDateTime createdAt
) {
}
