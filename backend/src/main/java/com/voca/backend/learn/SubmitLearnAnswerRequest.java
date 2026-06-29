package com.voca.backend.learn;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SubmitLearnAnswerRequest(
        @NotNull Long sessionItemId,
        @NotBlank String answer,
        @Min(0) Long responseTimeMs
) {}
