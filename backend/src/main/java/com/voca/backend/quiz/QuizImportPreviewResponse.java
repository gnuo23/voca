package com.voca.backend.quiz;

import java.util.List;

public record QuizImportPreviewResponse(
        List<QuizImportItemResponse> items,
        int validCount,
        int skippedCount,
        int errorCount
) {
}
