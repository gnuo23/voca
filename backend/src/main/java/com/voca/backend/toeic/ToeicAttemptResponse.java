package com.voca.backend.toeic;

import java.time.LocalDateTime;
import java.util.List;

public record ToeicAttemptResponse(
        Long id,
        Long testId,
        String testSlug,
        String testName,
        String mode,
        String partFilter,
        int totalQuestions,
        int answeredCount,
        String status,
        LocalDateTime startedAt,
        LocalDateTime expiresAt,
        List<ToeicGroupResponse> groups
) {
}
