package com.voca.backend.quiz;

import java.util.List;

public record QuizImportItemResponse(
        int lineNumber,
        Long vocabId,
        String word,
        String meaning,
        String prompt,
        List<QuestionType> questionTypes,
        QuizImportStatus status,
        String message
) {
}
