package com.voca.backend.learn;

public record LearnAnswerResponse(
        boolean correct,
        GradeVerdict verdict,
        double similarityScore,
        String userAnswer,
        String correctAnswer,
        LearnItemStage newStage,
        int correctStreak,
        LearnQuestionResponse.VocabContext vocab,
        LearnQuestionResponse.Progress progress
) {}
