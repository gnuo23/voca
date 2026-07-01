package com.voca.backend.deck;

import java.time.LocalDateTime;

public record DeckShareCodeResponse(
        Long deckId,
        String deckName,
        String code,
        LocalDateTime createdAt
) {
}
