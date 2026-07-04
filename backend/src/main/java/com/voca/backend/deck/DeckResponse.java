package com.voca.backend.deck;

import java.time.LocalDateTime;

public record DeckResponse(
        Long id,
        String name,
        String description,
        Integer totalWords,
        Integer learnedWords,
        Integer dueWords,
        Integer dueTodayCount,
        Integer savedQuestionCount,
        Long ownerId,
        String ownerName,
        boolean ownedByCurrentUser,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static DeckResponse from(Deck deck) {
        return from(deck, 0, 0, 0, 0, 0, deck.getOwner().getId(), deck.getOwner().getDisplayName(), true);
    }

    public static DeckResponse from(Deck deck, long totalWords, long learnedWords, long dueWords, long dueTodayCount, long savedQuestionCount) {
        return from(deck, totalWords, learnedWords, dueWords, dueTodayCount, savedQuestionCount, deck.getOwner().getId(), deck.getOwner().getDisplayName(), true);
    }

    public static DeckResponse from(
            Deck deck,
            long totalWords,
            long learnedWords,
            long dueWords,
            long dueTodayCount,
            long savedQuestionCount,
            Long currentUserId
    ) {
        return from(
                deck,
                totalWords,
                learnedWords,
                dueWords,
                dueTodayCount,
                savedQuestionCount,
                deck.getOwner().getId(),
                deck.getOwner().getDisplayName(),
                deck.getOwner().getId().equals(currentUserId)
        );
    }

    private static DeckResponse from(
            Deck deck,
            long totalWords,
            long learnedWords,
            long dueWords,
            long dueTodayCount,
            long savedQuestionCount,
            Long ownerId,
            String ownerName,
            boolean ownedByCurrentUser
    ) {
        return new DeckResponse(
                deck.getId(),
                deck.getName(),
                deck.getDescription(),
                Math.toIntExact(totalWords),
                Math.toIntExact(learnedWords),
                Math.toIntExact(dueWords),
                Math.toIntExact(dueTodayCount),
                Math.toIntExact(savedQuestionCount),
                ownerId,
                ownerName,
                ownedByCurrentUser,
                deck.getCreatedAt(),
                deck.getUpdatedAt()
        );
    }
}
