package com.voca.backend.toeic;

import java.util.List;

public record ToeicResultAnswerResponse(
        Long questionId,
        int questionNumber,
        String questionPart,
        String questionText,
        String selectedLabel,
        String correctAnswerLabel,
        boolean correct,
        boolean answered,
        List<ToeicAnswerOptionResponse> options,
        String audioTranscript,
        String explanationHtml
) {
}
