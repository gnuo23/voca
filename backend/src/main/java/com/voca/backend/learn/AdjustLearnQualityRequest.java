package com.voca.backend.learn;

import com.voca.backend.review.ReviewQuality;
import jakarta.validation.constraints.NotNull;

public record AdjustLearnQualityRequest(
        @NotNull Long sessionItemId,
        @NotNull ReviewQuality quality
) {
}
