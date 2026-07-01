package com.voca.backend.deck;

import java.time.LocalDateTime;

public record DeckResponse(
        Long id,
        String name,
        String description,
        Integer totalWords,
        Integer learnedWords,
        Integer dueWords,
        Integer savedQuestionCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static DeckResponse from(Deck deck) {
        return from(deck, 0, 0, 0, 0);
    }

    public static DeckResponse from(Deck deck, long totalWords, long learnedWords, long dueWords, long savedQuestionCount) {
        return new DeckResponse(
                deck.getId(),
                deck.getName(),
                deck.getDescription(),
                Math.toIntExact(totalWords),
                Math.toIntExact(learnedWords),
                Math.toIntExact(dueWords),
                Math.toIntExact(savedQuestionCount),
                deck.getCreatedAt(),
                deck.getUpdatedAt()
        );
    }
}
