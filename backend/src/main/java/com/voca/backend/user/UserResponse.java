package com.voca.backend.user;

public record UserResponse(
        Long id,
        String email,
        String displayName,
        EnglishLevel englishLevel,
        String learningGoal,
        Integer dailyGoal
) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getEnglishLevel(),
                user.getLearningGoal(),
                user.getDailyGoal()
        );
    }
}
