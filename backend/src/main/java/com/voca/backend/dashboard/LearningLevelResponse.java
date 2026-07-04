package com.voca.backend.dashboard;

public record LearningLevelResponse(
        int level,
        long xp,
        long xpForCurrentLevel,
        long xpForNextLevel,
        int progressPercent
) {
}
