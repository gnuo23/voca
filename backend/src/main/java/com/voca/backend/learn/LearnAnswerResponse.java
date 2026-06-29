package com.voca.backend.learn;

public record LearnAnswerResponse(
        boolean correct,
        String correctAnswer,
        LearnItemStage newStage,
        int correctStreak,
        LearnQuestionResponse.Progress progress
) {}
