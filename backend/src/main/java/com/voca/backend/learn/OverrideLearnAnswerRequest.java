package com.voca.backend.learn;

import jakarta.validation.constraints.NotNull;

public record OverrideLearnAnswerRequest(
        @NotNull Long sessionItemId,
        GradeVerdict verdict
) {
    public GradeVerdict targetVerdict() {
        return verdict == null ? GradeVerdict.CORRECT : verdict;
    }
}
