package com.voca.backend.toeic;

public record ToeicAnswerAckResponse(
        Long questionId,
        String selectedLabel,
        int answeredCount,
        int totalQuestions
) {
}
