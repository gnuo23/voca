package com.voca.backend.learn;

import java.util.List;

public record LearnQuestionResponse(
        Long sessionItemId,
        Long vocabId,
        String word,
        LearnQuestionType questionType,
        String questionToken,
        String prompt,
        List<String> options,
        String trueFalseStatement,
        LearnItemStage stage,
        Progress progress
) {
    public record Progress(
            int masteredTerms,
            int totalTerms,
            int remainingTerms,
            int newTerms,
            int seenTerms,
            int learningTerms,
            int familiarTerms
    ) {}
}
