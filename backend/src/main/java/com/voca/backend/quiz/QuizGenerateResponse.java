package com.voca.backend.quiz;

import java.util.List;

public record QuizGenerateResponse(
        Long deckId,
        int questionCount,
        List<QuizQuestionResponse> questions
) {
}
