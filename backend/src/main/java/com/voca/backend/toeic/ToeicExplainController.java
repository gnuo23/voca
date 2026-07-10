package com.voca.backend.toeic;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/toeic/questions")
public class ToeicExplainController {

    private final ToeicExplainService explainService;

    public ToeicExplainController(ToeicExplainService explainService) {
        this.explainService = explainService;
    }

    @PostMapping("/{questionId}/explain")
    public ToeicExplanationResponse explain(
            @PathVariable Long questionId,
            @Valid @RequestBody(required = false) ExplainToeicQuestionRequest request
    ) {
        return explainService.explain(questionId, request);
    }
}
