package com.voca.backend.vocab;

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
@RequestMapping("/api")
public class VocabItemController {

    private final VocabItemService vocabItemService;

    public VocabItemController(VocabItemService vocabItemService) {
        this.vocabItemService = vocabItemService;
    }

    @GetMapping("/decks/{deckId}/vocab")
    public List<VocabItemResponse> listByDeck(Authentication authentication, @PathVariable Long deckId) {
        return vocabItemService.listByDeck(authentication, deckId);
    }

    @GetMapping("/vocab/{vocabId}")
    public VocabItemResponse get(Authentication authentication, @PathVariable Long vocabId) {
        return vocabItemService.get(authentication, vocabId);
    }

    @GetMapping("/vocab/{vocabId}/audio")
    public VocabAudioResponse getAudio(Authentication authentication, @PathVariable Long vocabId) {
        return vocabItemService.getAudio(authentication, vocabId);
    }

    @PostMapping("/vocab/{vocabId}/refresh-audio")
    public VocabAudioResponse refreshAudio(Authentication authentication, @PathVariable Long vocabId) {
        return vocabItemService.refreshAudio(authentication, vocabId);
    }

    @PutMapping("/vocab/{vocabId}")
    public VocabItemResponse update(
            Authentication authentication,
            @PathVariable Long vocabId,
            @Valid @RequestBody VocabItemRequest request
    ) {
        return vocabItemService.update(authentication, vocabId, request);
    }

    @DeleteMapping("/vocab/{vocabId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Authentication authentication, @PathVariable Long vocabId) {
        vocabItemService.delete(authentication, vocabId);
    }

    @PostMapping("/vocab/{vocabId}/mark")
    public VocabItemResponse mark(
            Authentication authentication,
            @PathVariable Long vocabId,
            @Valid @RequestBody VocabMarkRequest request
    ) {
        return vocabItemService.mark(authentication, vocabId, request);
    }
}
