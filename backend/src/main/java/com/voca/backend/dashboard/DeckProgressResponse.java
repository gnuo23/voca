package com.voca.backend.dashboard;

public record DeckProgressResponse(
        Long deckId,
        String deckName,
        long totalWords,
        long newCount,
        long learningCount,
        long reviewCount,
        long difficultCount,
        long masteredCount,
        double progressScore
) {
}
