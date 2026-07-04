package com.voca.backend.deck;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/study-decks")
public class StudyDeckController {

    private final DeckService deckService;

    public StudyDeckController(DeckService deckService) {
        this.deckService = deckService;
    }

    @GetMapping
    public List<DeckResponse> list(Authentication authentication) {
        return deckService.listStudyDecks(authentication);
    }
}
