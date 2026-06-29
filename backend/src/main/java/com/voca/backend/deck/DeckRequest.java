package com.voca.backend.deck;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DeckRequest(
        @NotBlank @Size(max = 160) String name,
        @Size(max = 1000) String description
) {
}
