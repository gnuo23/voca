package com.voca.backend.vocab;

import java.util.List;

public record VocabImportConfirmResponse(
        Integer importedCount,
        List<VocabImportItemResponse> items,
        List<VocabImportErrorResponse> errors
) {
}
