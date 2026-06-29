package com.voca.backend.review;

import com.voca.backend.vocab.VocabProgressStatus;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/api/review/today")
    public ReviewTodayResponse today(
            Authentication authentication,
            @RequestParam(required = false) Long deckId,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) VocabProgressStatus status
    ) {
        return reviewService.today(authentication, deckId, limit, status);
    }

    @GetMapping("/api/review/schedule")
    public ReviewScheduleResponse schedule(
            Authentication authentication,
            @RequestParam(required = false) Long deckId,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) VocabProgressStatus status
    ) {
        return reviewService.schedule(authentication, deckId, limit, status);
    }

    @PostMapping("/api/review/{vocabId}/result")
    public ReviewProgressResponse submit(
            Authentication authentication,
            @PathVariable Long vocabId,
            @Valid @RequestBody ReviewResultRequest request
    ) {
        return reviewService.submit(authentication, vocabId, request);
    }
}
