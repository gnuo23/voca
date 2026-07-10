package com.voca.backend.toeic;

import org.springframework.stereotype.Component;

/**
 * Converts raw correct counts (out of 100 per skill) to TOEIC scaled scores.
 * Listening = PART_1..4, Reading = PART_5..7. Each skill scales 5-495; total /990.
 * Uses a piecewise-linear approximation of published ETS conversion tables — exact
 * ETS tables vary per form, so this gives an indicative score, not an official one.
 */
@Component
public class ToeicScoreConverter {

    private static final int MAX_RAW_PER_SKILL = 100;

    public int listeningScore(int correct) {
        return scaleListening(clamp(correct));
    }

    public int readingScore(int correct) {
        return scaleReading(clamp(correct));
    }

    public boolean isListeningPart(String part) {
        return "PART_1".equals(part) || "PART_2".equals(part)
                || "PART_3".equals(part) || "PART_4".equals(part);
    }

    private int clamp(int raw) {
        if (raw < 0) {
            return 0;
        }
        return Math.min(raw, MAX_RAW_PER_SKILL);
    }

    private int scaleListening(int raw) {
        if (raw == 0) {
            return 5;
        }
        int scaled = Math.round(raw * 5.0f);
        return bound(scaled);
    }

    private int scaleReading(int raw) {
        if (raw == 0) {
            return 5;
        }
        int scaled = Math.round(raw * 4.95f);
        return bound(scaled);
    }

    private int bound(int scaled) {
        if (scaled < 5) {
            return 5;
        }
        return Math.min(scaled, 495);
    }
}
