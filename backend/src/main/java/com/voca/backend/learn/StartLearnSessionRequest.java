package com.voca.backend.learn;

import jakarta.validation.constraints.NotNull;

public record StartLearnSessionRequest(
        @NotNull Long deckId,
        LearnSessionScope scope
) {}
