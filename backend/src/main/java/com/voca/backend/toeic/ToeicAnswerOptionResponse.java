package com.voca.backend.toeic;

public record ToeicAnswerOptionResponse(
        String label,
        String content,
        int answerOrder
) {
    public static ToeicAnswerOptionResponse from(ToeicAnswer answer) {
        return new ToeicAnswerOptionResponse(
                answer.getAnswerLabel(),
                answer.getContent(),
                answer.getAnswerOrder()
        );
    }
}
