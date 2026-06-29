package com.voca.backend.vocab;

public record VocabImportItemResponse(
        Integer lineNumber,
        String word,
        String partOfSpeech,
        String meaningVi,
        VocabImportStatus status,
        String message
) {
}
