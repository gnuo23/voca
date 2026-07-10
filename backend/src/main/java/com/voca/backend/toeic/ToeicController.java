package com.voca.backend.toeic;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/toeic")
public class ToeicController {

    private final ToeicService toeicService;

    public ToeicController(ToeicService toeicService) {
        this.toeicService = toeicService;
    }

    @GetMapping("/tests")
    public List<ToeicTestSummaryResponse> listTests() {
        return toeicService.listTests();
    }

    @GetMapping("/tests/{slug}")
    public ToeicTestSummaryResponse getTest(@PathVariable String slug) {
        return toeicService.getTest(slug);
    }
}
