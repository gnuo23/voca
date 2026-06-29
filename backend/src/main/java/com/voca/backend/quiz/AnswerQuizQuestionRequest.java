package com.voca.backend.quiz;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AnswerQuizQuestionRequest(
        @NotNull Long questionId,
        @Size(max = 1000) String answer,
        Integer responseTimeMs
) {
}
