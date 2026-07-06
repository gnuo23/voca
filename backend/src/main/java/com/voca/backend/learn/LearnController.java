package com.voca.backend.learn;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/learn")
public class LearnController {

    private final LearnService learnService;

    public LearnController(LearnService learnService) {
        this.learnService = learnService;
    }

    @GetMapping("/sessions/active")
    public ResponseEntity<LearnSessionResponse> getActiveSession(
            Authentication auth,
            @RequestParam Long deckId
    ) {
        return learnService.getActiveSession(auth, deckId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/sessions")
    public LearnSessionResponse startSession(
            Authentication auth,
            @Valid @RequestBody StartLearnSessionRequest request
    ) {
        return learnService.startSession(auth, request);
    }

    @GetMapping("/sessions/{id}/next")
    public LearnQuestionResponse getNextQuestion(
            Authentication auth,
            @PathVariable Long id
    ) {
        return learnService.getNextQuestion(auth, id);
    }

    @PostMapping("/sessions/{id}/answer")
    public LearnAnswerResponse submitAnswer(
            Authentication auth,
            @PathVariable Long id,
            @Valid @RequestBody SubmitLearnAnswerRequest request
    ) {
        return learnService.submitAnswer(auth, id, request);
    }

    @PostMapping("/sessions/{id}/override")
    public LearnAnswerResponse overrideAnswer(
            Authentication auth,
            @PathVariable Long id,
            @Valid @RequestBody OverrideLearnAnswerRequest request
    ) {
        return learnService.overrideAnswer(auth, id, request);
    }

    @PostMapping("/sessions/{id}/quality")
    public void adjustQuality(
            Authentication auth,
            @PathVariable Long id,
            @Valid @RequestBody AdjustLearnQualityRequest request
    ) {
        learnService.adjustQuality(auth, id, request);
    }

    @PostMapping("/decks/{deckId}/reset-progress")
    public void resetLearnProgress(
            Authentication auth,
            @PathVariable Long deckId
    ) {
        learnService.resetLearnProgress(auth, deckId);
    }

    @GetMapping("/sessions/{id}/result")
    public LearnSessionResultResponse getSessionResult(
            Authentication auth,
            @PathVariable Long id
    ) {
        return learnService.getSessionResult(auth, id);
    }
}
