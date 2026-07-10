package com.voca.backend.toeic;

import java.util.List;

public record ToeicQuestionResponse(
        Long id,
        int questionNumber,
        String questionPart,
        String questionText,
        List<ToeicAnswerOptionResponse> options
) {
    public static ToeicQuestionResponse from(ToeicQuestion question) {
        return new ToeicQuestionResponse(
                question.getId(),
                question.getQuestionNumber(),
                question.getQuestionPart(),
                question.getQuestionText(),
                question.getAnswers().stream().map(ToeicAnswerOptionResponse::from).toList()
        );
    }
}
