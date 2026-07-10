package com.voca.backend.toeic;

import jakarta.validation.constraints.Size;

public record ExplainToeicQuestionRequest(
        @Size(max = 2000, message = "Question must not exceed 2000 characters")
        String userQuestion
) {
}
