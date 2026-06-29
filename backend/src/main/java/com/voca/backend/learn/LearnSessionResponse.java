package com.voca.backend.learn;

import java.time.LocalDateTime;

public record LearnSessionResponse(
        Long id,
        Long deckId,
        String deckName,
        int totalTerms,
        int masteredTerms,
        int totalAnswers,
        int correctAnswers,
        LearnSessionStatus status,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        long durationMs
) {
    public static LearnSessionResponse from(LearnSession session) {
        return new LearnSessionResponse(
                session.getId(),
                session.getDeck().getId(),
                session.getDeck().getName(),
                session.getTotalTerms(),
                session.getMasteredTerms(),
                session.getTotalAnswers(),
                session.getCorrectAnswers(),
                session.getStatus(),
                session.getStartedAt(),
                session.getCompletedAt(),
                session.getDurationMs()
        );
    }
}
