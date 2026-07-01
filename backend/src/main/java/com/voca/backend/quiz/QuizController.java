package com.voca.backend.quiz;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class QuizController {

    private final QuizService quizService;

    public QuizController(QuizService quizService) {
        this.quizService = quizService;
    }

    @PostMapping("/api/decks/{deckId}/quiz/import/preview")
    public QuizImportPreviewResponse previewImport(
            Authentication authentication,
            @PathVariable Long deckId,
            @RequestBody QuizImportRequest request
    ) {
        return quizService.previewImport(authentication, deckId, request);
    }

    @PostMapping("/api/decks/{deckId}/quiz/import/save")
    public QuizImportPreviewResponse saveImport(
            Authentication authentication,
            @PathVariable Long deckId,
            @RequestBody QuizImportRequest request
    ) {
        return quizService.saveImport(authentication, deckId, request);
    }

    @PostMapping("/api/decks/{deckId}/quiz/start")
    public QuizAttemptResponse startQuiz(
            Authentication authentication,
            @PathVariable Long deckId,
            @RequestBody(required = false) StartQuizRequest request
    ) {
        return quizService.startQuiz(authentication, deckId, request);
    }

    @PostMapping("/api/quiz-attempts/{attemptId}/answer")
    public QuizAnswerResponse answer(
            Authentication authentication,
            @PathVariable Long attemptId,
            @Valid @RequestBody AnswerQuizQuestionRequest request
    ) {
        return quizService.answer(authentication, attemptId, request);
    }

    @GetMapping("/api/quiz-attempts/{attemptId}/result")
    public QuizResultResponse result(Authentication authentication, @PathVariable Long attemptId) {
        return quizService.result(authentication, attemptId);
    }
}
