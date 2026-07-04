package com.voca.backend.classroom;

import java.time.LocalDateTime;

public record ClassroomMemberProgressResponse(
        Long userId,
        String displayName,
        String email,
        ClassroomRole role,
        long totalWords,
        long touchedWords,
        long learnedWords,
        long masteredWords,
        long reviewWords,
        long difficultWords,
        long correctAnswers,
        long wrongAnswers,
        int accuracyPercent,
        LocalDateTime lastActivityAt,
        LocalDateTime joinedAt
) {
}
