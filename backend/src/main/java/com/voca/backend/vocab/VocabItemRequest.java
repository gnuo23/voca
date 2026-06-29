package com.voca.backend.vocab;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VocabItemRequest(
        @NotBlank @Size(max = 255) String word,
        @Size(max = 80) String partOfSpeech,
        @Size(max = 1000) String meaningVi
) {
}
