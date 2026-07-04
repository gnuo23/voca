package com.voca.backend.dashboard;

import java.time.LocalDate;

public record WeeklyStatResponse(
        String label,
        LocalDate date,
        long learned,
        long reviewed,
        long total
) {
}
