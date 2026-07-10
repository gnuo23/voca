package com.voca.backend.toeic;

public record ToeicPartBreakdownResponse(
        String part,
        int total,
        int correct,
        int accuracyPercent
) {
}
