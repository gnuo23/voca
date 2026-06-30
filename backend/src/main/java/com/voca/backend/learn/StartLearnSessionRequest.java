package com.voca.backend.learn;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record StartLearnSessionRequest(
        @NotNull Long deckId,
        LearnSessionScope scope,
        LearnGoal goal,
        LearnAnswerDirection answerDirection,
        LearnGradingMode gradingMode,
        List<LearnQuestionType> questionTypes
) {}
