package com.voca.backend.review;

import jakarta.validation.constraints.Min;

public record ReviewResultRequest(
        ReviewQuality quality,
        Boolean isCorrect,
        @Min(0) Integer responseTimeMs,
        ReviewSource source
) {
}
