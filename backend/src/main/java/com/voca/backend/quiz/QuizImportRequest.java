package com.voca.backend.quiz;

import java.util.List;

public record QuizImportRequest(
        String rawText,
        List<QuestionType> questionTypes,
        Integer limit
) {
}
