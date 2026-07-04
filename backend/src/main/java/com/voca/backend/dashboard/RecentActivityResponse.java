package com.voca.backend.dashboard;

import java.time.LocalDateTime;

public record RecentActivityResponse(
        String type,
        String title,
        String description,
        Long deckId,
        Long vocabId,
        LocalDateTime occurredAt
) {
}
