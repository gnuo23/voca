package com.voca.backend.toeic;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/toeic")
public class ToeicAttemptController {

    private final ToeicAttemptService attemptService;

    public ToeicAttemptController(ToeicAttemptService attemptService) {
        this.attemptService = attemptService;
    }

    @PostMapping("/tests/{slug}/attempts")
    public ToeicAttemptResponse start(
            Authentication authentication,
            @PathVariable String slug,
            @RequestBody(required = false) StartToeicAttemptRequest request
    ) {
        return attemptService.start(authentication, slug, request);
    }

    @GetMapping("/attempts/{attemptId}")
    public ToeicAttemptResponse getAttempt(Authentication authentication, @PathVariable Long attemptId) {
        return attemptService.getAttempt(authentication, attemptId);
    }

    @PostMapping("/attempts/{attemptId}/answer")
    public ToeicAnswerAckResponse answer(
            Authentication authentication,
            @PathVariable Long attemptId,
            @Valid @RequestBody AnswerToeicQuestionRequest request
    ) {
        return attemptService.answer(authentication, attemptId, request);
    }

    @PostMapping("/attempts/{attemptId}/submit")
    public ToeicResultResponse submit(Authentication authentication, @PathVariable Long attemptId) {
        return attemptService.submit(authentication, attemptId);
    }

    @GetMapping("/attempts/{attemptId}/result")
    public ToeicResultResponse result(Authentication authentication, @PathVariable Long attemptId) {
        return attemptService.result(authentication, attemptId);
    }
}
