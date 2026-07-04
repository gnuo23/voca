package com.voca.backend.classroom;

import jakarta.validation.constraints.NotNull;

public record AddClassroomDeckRequest(
        @NotNull Long deckId
) {
}
