package com.voca.backend.quiz;

public record QuizImportItemResponse(
        int lineNumber,
        Long vocabId,
        String word,
        String prompt,
        QuizImportStatus status,
        String message
) {
}
