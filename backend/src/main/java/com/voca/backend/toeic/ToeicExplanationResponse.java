package com.voca.backend.toeic;

public record ToeicExplanationResponse(
        Long questionId,
        String answer
) {
}
