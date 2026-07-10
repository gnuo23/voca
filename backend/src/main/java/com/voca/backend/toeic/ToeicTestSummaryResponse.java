package com.voca.backend.toeic;

import java.util.Map;

public record ToeicTestSummaryResponse(
        Long id,
        String slug,
        String testName,
        String collectionName,
        Integer testNumber,
        int totalQuestions,
        int durationMinutes,
        Map<String, Integer> partQuestionCount
) {
    public static ToeicTestSummaryResponse from(ToeicTest test, Map<String, Integer> partQuestionCount) {
        return new ToeicTestSummaryResponse(
                test.getId(),
                test.getSlug(),
                test.getTestName(),
                test.getCollectionName(),
                test.getTestNumber(),
                test.getTotalQuestions(),
                test.getDurationMinutes(),
                partQuestionCount
        );
    }
}
