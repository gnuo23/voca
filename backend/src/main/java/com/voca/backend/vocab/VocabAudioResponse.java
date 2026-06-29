package com.voca.backend.vocab;

import java.time.LocalDateTime;

public record VocabAudioResponse(
        Long vocabId,
        String word,
        String audioUrl,
        String audioUsUrl,
        String audioUkUrl,
        String audioAccent,
        String audioSource,
        LocalDateTime audioRefreshedAt
) {
    static VocabAudioResponse from(VocabItem item) {
        return new VocabAudioResponse(
                item.getId(),
                item.getWord(),
                item.getAudioUrl(),
                item.getAudioUsUrl(),
                item.getAudioUkUrl(),
                item.getAudioAccent(),
                item.getAudioSource(),
                item.getAudioRefreshedAt()
        );
    }
}
