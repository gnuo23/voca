package com.voca.backend.vocab;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record VocabImportRequest(
        @NotNull Long deckId,
        @NotBlank @Size(max = 20000) String rawText
) {
}
