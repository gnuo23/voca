package com.voca.backend.vocab;

public record VocabImportErrorResponse(
        Integer lineNumber,
        String message
) {
}
