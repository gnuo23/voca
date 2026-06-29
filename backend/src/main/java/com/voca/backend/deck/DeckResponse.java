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
        return new DeckResponse(
                deck.getId(),
                deck.getName(),
                deck.getDescription(),
                0,
                0,
                0,
                deck.getCreatedAt(),
                deck.getUpdatedAt()
        );
    }
}
