package com.voca.backend.review;

import java.util.List;

public record ReviewScheduleResponse(
        List<ReviewScheduleItemResponse> items,
        long totalItems,
        long dueNow,
        long overdue,
        long upcoming,
        long newItems
) {
}
