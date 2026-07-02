package com.voca.backend.learn;

import com.voca.backend.review.ReviewQuality;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SubmitLearnAnswerRequest(
        @NotNull Long sessionItemId,
        @NotNull String answer,
        LearnQuestionType questionType,
        @Min(0) Long responseTimeMs,
        String questionToken,
        ReviewQuality quality
) {
    public SubmitLearnAnswerRequest(Long sessionItemId, String answer, LearnQuestionType questionType, Long responseTimeMs) {
        this(sessionItemId, answer, questionType, responseTimeMs, null, null);
    }

    public SubmitLearnAnswerRequest(Long sessionItemId, String answer, LearnQuestionType questionType, Long responseTimeMs, String questionToken) {
        this(sessionItemId, answer, questionType, responseTimeMs, questionToken, null);
    }
}
