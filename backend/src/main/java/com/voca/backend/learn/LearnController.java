package com.voca.backend.learn;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/learn")
public class LearnController {

    private final LearnService learnService;

    public LearnController(LearnService learnService) {
        this.learnService = learnService;
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

    @GetMapping("/sessions/{id}/result")
    public LearnSessionResultResponse getSessionResult(
            Authentication auth,
            @PathVariable Long id
    ) {
        return learnService.getSessionResult(auth, id);
    }
}
