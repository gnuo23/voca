package com.voca.backend.user;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank @Size(max = 120) String displayName,
        @NotNull EnglishLevel englishLevel,
        @Size(max = 500) String learningGoal,
        @NotNull @Min(1) @Max(200) Integer dailyGoal
) {
}
