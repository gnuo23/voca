package com.voca.backend.toeic;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ToeicService {

    private final ToeicTestRepository testRepository;
    private final ToeicQuestionRepository questionRepository;

    public ToeicService(ToeicTestRepository testRepository, ToeicQuestionRepository questionRepository) {
        this.testRepository = testRepository;
        this.questionRepository = questionRepository;
    }

    @Transactional(readOnly = true)
    public List<ToeicTestSummaryResponse> listTests() {
        return testRepository.findAllByOrderByCollectionNameAscTestNumberAsc().stream()
                .map(test -> ToeicTestSummaryResponse.from(test, partCounts(test.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public ToeicTestSummaryResponse getTest(String slug) {
        ToeicTest test = testRepository.findBySlug(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "TOEIC test not found"));
        return ToeicTestSummaryResponse.from(test, partCounts(test.getId()));
    }

    private Map<String, Integer> partCounts(Long testId) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (ToeicQuestionRepository.PartCount row : questionRepository.countByPart(testId)) {
            counts.put(row.getPart(), (int) row.getTotal());
        }
        return counts;
    }
}
