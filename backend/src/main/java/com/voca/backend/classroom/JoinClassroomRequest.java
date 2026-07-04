package com.voca.backend.classroom;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JoinClassroomRequest(
        @NotBlank @Size(max = 32) String code
) {
}
