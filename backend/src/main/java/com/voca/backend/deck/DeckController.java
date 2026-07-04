package com.voca.backend.deck;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/decks")
public class DeckController {

    private final DeckService deckService;

    public DeckController(DeckService deckService) {
        this.deckService = deckService;
    }

    @PostMapping
    public DeckResponse create(Authentication authentication, @Valid @RequestBody DeckRequest request) {
        return deckService.create(authentication, request);
    }

    @GetMapping
    public List<DeckResponse> list(Authentication authentication) {
        return deckService.list(authentication);
    }

    @GetMapping("/study")
    public List<DeckResponse> listStudyDecks(Authentication authentication) {
        return deckService.listStudyDecks(authentication);
    }

    @GetMapping("/{deckId}")
    public DeckResponse get(Authentication authentication, @PathVariable Long deckId) {
        return deckService.get(authentication, deckId);
    }

    @PutMapping("/{deckId}")
    public DeckResponse update(
            Authentication authentication,
            @PathVariable Long deckId,
            @Valid @RequestBody DeckRequest request
    ) {
        return deckService.update(authentication, deckId, request);
    }

    @DeleteMapping("/{deckId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Authentication authentication, @PathVariable Long deckId) {
        deckService.delete(authentication, deckId);
    }

    @PostMapping("/{deckId}/reset-progress")
    public DeckResponse resetProgress(Authentication authentication, @PathVariable Long deckId) {
        return deckService.resetDeckProgress(authentication, deckId);
    }
}
