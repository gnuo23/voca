package com.voca.backend.toeic;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AnswerToeicQuestionRequest(
        @NotNull Long questionId,
        @Size(max = 4) String selectedLabel
) {
}
