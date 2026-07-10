package com.voca.backend.toeic;

public record StartToeicAttemptRequest(
        String mode,
        String partFilter
) {
}
