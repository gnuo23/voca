package com.voca.backend.deck;

import java.time.LocalDateTime;

public record DeckResponse(
        Long id,
        String name,
        String description,
        Integer totalWords,
        Integer learnedWords,
        Integer dueWords,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static DeckResponse from(Deck deck) {
        return from(deck, 0);
    }

    public static DeckResponse from(Deck deck, long totalWords) {
        return new DeckResponse(
                deck.getId(),
                deck.getName(),
                deck.getDescription(),
                Math.toIntExact(totalWords),
                0,
                0,
                deck.getCreatedAt(),
                deck.getUpdatedAt()
        );
    }
}
