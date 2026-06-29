package com.voca.backend.vocab;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/vocab/import")
public class VocabImportController {

    private final VocabImportService vocabImportService;

    public VocabImportController(VocabImportService vocabImportService) {
        this.vocabImportService = vocabImportService;
    }

    @PostMapping("/preview")
    public VocabImportPreviewResponse preview(
            Authentication authentication,
            @Valid @RequestBody VocabImportRequest request
    ) {
        return vocabImportService.preview(authentication, request);
    }

    @PostMapping("/confirm")
    public VocabImportConfirmResponse confirm(
            Authentication authentication,
            @Valid @RequestBody VocabImportRequest request
    ) {
        return vocabImportService.confirm(authentication, request);
    }
}
