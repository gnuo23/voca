package com.voca.backend.classroom;

import java.time.LocalDateTime;

public record ClassroomDeckResponse(
        Long deckId,
        String deckName,
        String description,
        long totalWords,
        long learnedWords,
        long dueWords,
        long dueTodayCount,
        LocalDateTime addedAt
) {
}
