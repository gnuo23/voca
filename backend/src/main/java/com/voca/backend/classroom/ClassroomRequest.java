package com.voca.backend.classroom;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClassroomRequest(
        @NotBlank @Size(max = 160) String name,
        @Size(max = 1000) String description
) {
}
