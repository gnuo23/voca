package com.voca.backend.toeic;

import com.voca.backend.user.User;
import com.voca.backend.user.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ToeicAttemptService {

    private static final String MODE_FULL = "FULL";
    private static final String MODE_PART = "PART";
    private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String STATUS_COMPLETED = "COMPLETED";

    private final UserService userService;
    private final ToeicTestRepository testRepository;
    private final ToeicQuestionGroupRepository groupRepository;
    private final ToeicQuestionRepository questionRepository;
    private final ToeicAttemptRepository attemptRepository;
    private final ToeicAttemptAnswerRepository attemptAnswerRepository;
    private final ToeicScoreConverter scoreConverter;

    public ToeicAttemptService(
            UserService userService,
            ToeicTestRepository testRepository,
            ToeicQuestionGroupRepository groupRepository,
            ToeicQuestionRepository questionRepository,
            ToeicAttemptRepository attemptRepository,
            ToeicAttemptAnswerRepository attemptAnswerRepository,
            ToeicScoreConverter scoreConverter
    ) {
        this.userService = userService;
        this.testRepository = testRepository;
        this.groupRepository = groupRepository;
        this.questionRepository = questionRepository;
        this.attemptRepository = attemptRepository;
        this.attemptAnswerRepository = attemptAnswerRepository;
        this.scoreConverter = scoreConverter;
    }

    @Transactional
    public ToeicAttemptResponse start(Authentication authentication, String slug, StartToeicAttemptRequest request) {
        User user = userService.currentUser(authentication);
        ToeicTest test = testRepository.findBySlug(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "TOEIC test not found"));

        String mode = resolveMode(request);
        String partFilter = MODE_PART.equals(mode) ? resolvePartFilter(request) : null;

        List<ToeicQuestionGroup> groups = groupRepository.findAllByTestIdOrderByGroupOrderAsc(test.getId());
        if (MODE_PART.equals(mode)) {
            String filter = partFilter;
            groups = groups.stream().filter(g -> filter.equals(g.getQuestionPart())).toList();
        }
        int totalQuestions = groups.stream().mapToInt(g -> g.getQuestions().size()).sum();
        if (totalQuestions == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No questions found for this attempt");
        }

        ToeicAttempt attempt = new ToeicAttempt();
        attempt.setUser(user);
        attempt.setTest(test);
        attempt.setMode(mode);
        attempt.setPartFilter(partFilter);
        attempt.setTotalQuestions(totalQuestions);
        attempt.setStatus(STATUS_IN_PROGRESS);
        attempt.setStartedAt(LocalDateTime.now());
        attempt.setExpiresAt(LocalDateTime.now().plusMinutes(test.getDurationMinutes()));
        ToeicAttempt saved = attemptRepository.save(attempt);

        return toAttemptResponse(saved, test, groups, 0);
    }

    @Transactional
    public ToeicAttemptResponse getAttempt(Authentication authentication, Long attemptId) {
        User user = userService.currentUser(authentication);
        ToeicAttempt attempt = findOwnedAttempt(user, attemptId);
        ToeicTest test = attempt.getTest();
        List<ToeicQuestionGroup> groups = loadAttemptGroups(attempt);
        int answered = (int) attemptAnswerRepository.countByAttemptId(attempt.getId());
        return toAttemptResponse(attempt, test, groups, answered);
    }

    @Transactional
    public ToeicAnswerAckResponse answer(Authentication authentication, Long attemptId, AnswerToeicQuestionRequest request) {
        User user = userService.currentUser(authentication);
        ToeicAttempt attempt = findOwnedAttempt(user, attemptId);
        if (STATUS_COMPLETED.equals(attempt.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Attempt already submitted");
        }
        ToeicQuestion question = questionRepository.findById(request.questionId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found"));
        if (!question.getTest().getId().equals(attempt.getTest().getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Question does not belong to this attempt");
        }
        if (MODE_PART.equals(attempt.getMode()) && !question.getQuestionPart().equals(attempt.getPartFilter())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Question is not in this attempt part");
        }

        String selected = request.selectedLabel() == null ? null : request.selectedLabel().trim();
        boolean correct = selected != null && selected.equalsIgnoreCase(question.getCorrectAnswerLabel());

        ToeicAttemptAnswer existing = attemptAnswerRepository
                .findByAttemptIdAndQuestionId(attempt.getId(), question.getId())
                .orElse(null);
        if (existing == null) {
            ToeicAttemptAnswer answer = new ToeicAttemptAnswer();
            answer.setAttempt(attempt);
            answer.setQuestion(question);
            answer.setSelectedLabel(selected);
            answer.setCorrect(correct);
            attemptAnswerRepository.save(answer);
        } else {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Question has already been answered");
        }

        int answered = (int) attemptAnswerRepository.countByAttemptId(attempt.getId());
        return new ToeicAnswerAckResponse(question.getId(), selected, answered, attempt.getTotalQuestions());
    }

    @Transactional
    public ToeicResultResponse submit(Authentication authentication, Long attemptId) {
        User user = userService.currentUser(authentication);
        ToeicAttempt attempt = findOwnedAttempt(user, attemptId);
        grade(attempt);
        return buildResult(attempt);
    }

    @Transactional(readOnly = true)
    public ToeicResultResponse result(Authentication authentication, Long attemptId) {
        User user = userService.currentUser(authentication);
        ToeicAttempt attempt = findOwnedAttempt(user, attemptId);
        return buildResult(attempt);
    }

    private void grade(ToeicAttempt attempt) {
        List<ToeicAttemptAnswer> answers = attemptAnswerRepository.findAllByAttemptIdOrderByIdAsc(attempt.getId());
        int correct = 0;
        int listeningCorrect = 0;
        int readingCorrect = 0;
        for (ToeicAttemptAnswer answer : answers) {
            if (answer.isCorrect()) {
                correct++;
                if (scoreConverter.isListeningPart(answer.getQuestion().getQuestionPart())) {
                    listeningCorrect++;
                } else {
                    readingCorrect++;
                }
            }
        }
        attempt.setCorrectCount(correct);
        attempt.setListeningCorrect(listeningCorrect);
        attempt.setReadingCorrect(readingCorrect);
        if (MODE_FULL.equals(attempt.getMode())) {
            int listeningScore = scoreConverter.listeningScore(listeningCorrect);
            int readingScore = scoreConverter.readingScore(readingCorrect);
            attempt.setListeningScore(listeningScore);
            attempt.setReadingScore(readingScore);
            attempt.setScaledScore(listeningScore + readingScore);
        }
        attempt.setStatus(STATUS_COMPLETED);
        if (attempt.getCompletedAt() == null) {
            attempt.setCompletedAt(LocalDateTime.now());
        }
    }

    private ToeicResultResponse buildResult(ToeicAttempt attempt) {
        List<ToeicQuestionGroup> groups = loadAttemptGroups(attempt);
        Map<Long, ToeicAttemptAnswer> answerByQuestion = new LinkedHashMap<>();
        for (ToeicAttemptAnswer answer : attemptAnswerRepository.findAllByAttemptIdOrderByIdAsc(attempt.getId())) {
            answerByQuestion.put(answer.getQuestion().getId(), answer);
        }

        List<ToeicResultAnswerResponse> answers = new ArrayList<>();
        Map<String, int[]> partStats = new LinkedHashMap<>();
        for (ToeicQuestionGroup group : groups) {
            for (ToeicQuestion question : group.getQuestions()) {
                ToeicAttemptAnswer answer = answerByQuestion.get(question.getId());
                boolean answered = answer != null;
                boolean correct = answered && answer.isCorrect();
                String part = question.getQuestionPart();
                int[] stat = partStats.computeIfAbsent(part, k -> new int[2]);
                stat[0]++;
                if (correct) {
                    stat[1]++;
                }
                answers.add(new ToeicResultAnswerResponse(
                        question.getId(),
                        question.getQuestionNumber(),
                        part,
                        question.getQuestionText(),
                        answered ? answer.getSelectedLabel() : null,
                        question.getCorrectAnswerLabel(),
                        correct,
                        answered,
                        question.getAnswers().stream().map(ToeicAnswerOptionResponse::from).toList(),
                        group.getAudioTranscript(),
                        group.getExplanationHtml()
                ));
            }
        }

        List<ToeicPartBreakdownResponse> breakdown = new ArrayList<>();
        partStats.forEach((part, stat) -> {
            int total = stat[0];
            int correct = stat[1];
            int accuracy = total == 0 ? 0 : Math.round((correct * 100f) / total);
            breakdown.add(new ToeicPartBreakdownResponse(part, total, correct, accuracy));
        });

        ToeicTest test = attempt.getTest();
        return new ToeicResultResponse(
                attempt.getId(),
                test.getId(),
                test.getSlug(),
                test.getTestName(),
                attempt.getMode(),
                attempt.getPartFilter(),
                attempt.getTotalQuestions(),
                answers.size() == 0 ? 0 : (int) answerByQuestion.values().size(),
                attempt.getCorrectCount(),
                attempt.getListeningCorrect(),
                attempt.getReadingCorrect(),
                attempt.getListeningScore(),
                attempt.getReadingScore(),
                attempt.getScaledScore(),
                STATUS_COMPLETED.equals(attempt.getStatus()),
                attempt.getStartedAt(),
                attempt.getCompletedAt(),
                breakdown,
                answers
        );
    }

    private List<ToeicQuestionGroup> loadAttemptGroups(ToeicAttempt attempt) {
        List<ToeicQuestionGroup> groups = groupRepository.findAllByTestIdOrderByGroupOrderAsc(attempt.getTest().getId());
        if (MODE_PART.equals(attempt.getMode())) {
            String filter = attempt.getPartFilter();
            return groups.stream().filter(g -> filter.equals(g.getQuestionPart())).toList();
        }
        return groups;
    }

    private ToeicAttemptResponse toAttemptResponse(ToeicAttempt attempt, ToeicTest test, List<ToeicQuestionGroup> groups, int answeredCount) {
        return new ToeicAttemptResponse(
                attempt.getId(),
                test.getId(),
                test.getSlug(),
                test.getTestName(),
                attempt.getMode(),
                attempt.getPartFilter(),
                attempt.getTotalQuestions(),
                answeredCount,
                attempt.getStatus(),
                attempt.getStartedAt(),
                attempt.getExpiresAt(),
                groups.stream().map(ToeicGroupResponse::from).toList()
        );
    }

    private ToeicAttempt findOwnedAttempt(User user, Long attemptId) {
        return attemptRepository.findByIdAndUserId(attemptId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "TOEIC attempt not found"));
    }

    private String resolveMode(StartToeicAttemptRequest request) {
        if (request == null || request.mode() == null || request.mode().isBlank()) {
            return MODE_FULL;
        }
        String mode = request.mode().trim().toUpperCase();
        if (!MODE_FULL.equals(mode) && !MODE_PART.equals(mode)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "mode must be FULL or PART");
        }
        return mode;
    }

    private String resolvePartFilter(StartToeicAttemptRequest request) {
        if (request == null || request.partFilter() == null || request.partFilter().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "partFilter is required for PART mode");
        }
        return request.partFilter().trim().toUpperCase();
    }
}
