package com.voca.backend.deck;

public record DeckSharePreviewResponse(
        String code,
        String deckName,
        String description,
        String ownerName,
        int totalWords,
        int totalQuestions
) {
}
