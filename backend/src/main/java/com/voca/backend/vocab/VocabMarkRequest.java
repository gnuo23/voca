package com.voca.backend.vocab;

import jakarta.validation.constraints.NotNull;

public record VocabMarkRequest(
        @NotNull VocabMarkAction action
) {
}
