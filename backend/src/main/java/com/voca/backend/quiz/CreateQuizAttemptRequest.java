package com.voca.backend.quiz;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateQuizAttemptRequest(
        @NotNull Long deckId,
        @NotEmpty List<Long> questionIds
) {
}
