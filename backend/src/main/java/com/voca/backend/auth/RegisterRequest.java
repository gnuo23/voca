package com.voca.backend.auth;

import com.voca.backend.user.EnglishLevel;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @Email @NotBlank @Size(max = 255) String email,
        @NotBlank @Size(min = 8, max = 100) String password,
        @NotBlank @Size(max = 120) String displayName,
        @NotNull EnglishLevel englishLevel,
        @Size(max = 500) String learningGoal,
        @NotNull @Min(1) @Max(200) Integer dailyGoal
) {
}
