package com.voca.backend.toeic;

public record ToeicPartProgressResponse(
        String part,
        long answered,
        long correct,
        int accuracyPercent
) {
}
