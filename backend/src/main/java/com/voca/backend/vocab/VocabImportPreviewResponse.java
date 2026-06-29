package com.voca.backend.vocab;

import java.util.List;

public record VocabImportPreviewResponse(
        List<VocabImportItemResponse> items,
        List<VocabImportErrorResponse> errors
) {

    public boolean hasBlockingErrors() {
        return !errors.isEmpty() || items.stream().anyMatch(item -> item.status() != VocabImportStatus.OK);
    }
}
