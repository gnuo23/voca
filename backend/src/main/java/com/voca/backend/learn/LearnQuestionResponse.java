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
        String hint,
        LearnItemStage stage,
        VocabContext vocab,
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

    public record VocabContext(
            String ipa,
            String meaningVi,
            String partOfSpeech,
            String exampleEn,
            String exampleVi
    ) {}
}
