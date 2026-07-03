package com.voca.backend.dashboard;

import java.time.LocalDate;

public record StreakDayResponse(
        String label,
        LocalDate date,
        boolean active,
        boolean today
) {
}
