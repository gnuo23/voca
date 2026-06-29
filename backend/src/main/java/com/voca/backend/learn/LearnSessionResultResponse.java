package com.voca.backend.learn;

import java.time.LocalDateTime;
import java.util.List;

public record LearnSessionResultResponse(
        LearnSessionResponse session,
        List<ItemSummary> items,
        List<AnswerSummary> history
) {
    public record ItemSummary(
            Long vocabId,
            String word,
            String partOfSpeech,
            String meaningVi,
            LearnItemStage stage,
            int correctAttempts,
            int incorrectAttempts,
            int totalAttempts
    ) {}

    public record AnswerSummary(
            LearnQuestionType questionType,
            String prompt,
            String userAnswer,
            String correctAnswer,
            boolean correct,
            Long responseTimeMs,
            LocalDateTime answeredAt
    ) {}
}
