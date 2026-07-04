package com.voca.backend.classroom;

import java.time.LocalDateTime;
import java.util.List;

public record ClassroomResponse(
        Long id,
        String name,
        String description,
        String inviteCode,
        ClassroomRole role,
        long deckCount,
        long memberCount,
        long totalWords,
        long learnedWords,
        List<ClassroomDeckResponse> decks,
        List<ClassroomMemberProgressResponse> members,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
