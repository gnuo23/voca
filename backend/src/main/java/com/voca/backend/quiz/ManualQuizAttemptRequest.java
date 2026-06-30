package com.voca.backend.quiz;

import java.util.List;
import java.util.Map;

public record ManualQuizAttemptRequest(
        List<ManualQuestionRequest> questions,
        List<VocabPairRequest> vocabPairs,
        List<QuestionType> questionTypes,
        Integer limit
) {
    public record ManualQuestionRequest(
            Long vocabId,
            String word,
            QuestionType type,
            String prompt,
            List<String> options,
            Map<String, List<String>> matchingOptions,
            String correctAnswer,
            Map<String, String> correctPairs,
            String explanation
    ) {
    }

    public record VocabPairRequest(
            Long vocabId,
            String word,
            String meaning
    ) {
    }
}
