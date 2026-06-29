package com.voca.backend.review;

import java.util.List;

public record ReviewTodayResponse(
        List<ReviewItemResponse> items,
        long totalDue
) {
}
