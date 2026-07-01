package com.voca.backend.deck;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class DeckShareController {

    private final DeckShareService deckShareService;

    public DeckShareController(DeckShareService deckShareService) {
        this.deckShareService = deckShareService;
    }

    @GetMapping("/api/decks/{deckId}/share-code")
    public DeckShareCodeResponse getShareCode(Authentication authentication, @PathVariable Long deckId) {
        return deckShareService.getOrCreateShareCode(authentication, deckId);
    }

    @PostMapping("/api/decks/{deckId}/share-code/rotate")
    public DeckShareCodeResponse rotateShareCode(Authentication authentication, @PathVariable Long deckId) {
        return deckShareService.rotateShareCode(authentication, deckId);
    }

    @PostMapping("/api/deck-shares/preview")
    public DeckSharePreviewResponse previewShare(
            Authentication authentication,
            @RequestBody DeckShareImportRequest request
    ) {
        return deckShareService.previewImport(authentication, request);
    }

    @PostMapping("/api/deck-shares/import")
    public DeckResponse importShare(
            Authentication authentication,
            @RequestBody DeckShareImportRequest request
    ) {
        return deckShareService.importDeck(authentication, request);
    }
}
