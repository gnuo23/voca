package com.voca.backend.dashboard;

import com.voca.backend.vocab.VocabProgressStatus;

public record HardWordResponse(
        Long vocabId,
        String word,
        String meaningVi,
        int wrongCount,
        int lapseCount,
        VocabProgressStatus status
) {
}
