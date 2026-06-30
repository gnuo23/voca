package com.voca.backend.quiz;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.voca.backend.deck.Deck;
import com.voca.backend.deck.DeckService;
import com.voca.backend.review.ReviewService;
import com.voca.backend.user.User;
import com.voca.backend.user.UserService;
import com.voca.backend.vocab.VocabItem;
import com.voca.backend.vocab.VocabItemRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class QuizService {

    private static final int QUESTION_LIMIT = 10;
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };
    private static final TypeReference<Map<String, List<String>>> MATCHING_OPTIONS = new TypeReference<>() {
    };

    private final DeckService deckService;
    private final UserService userService;
    private final VocabItemRepository vocabItemRepository;
    private final QuestionRepository questionRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final QuizAnswerRepository quizAnswerRepository;
    private final ReviewService reviewService;
    private final ObjectMapper objectMapper;

    public QuizService(
            DeckService deckService,
            UserService userService,
            VocabItemRepository vocabItemRepository,
            QuestionRepository questionRepository,
            QuizAttemptRepository quizAttemptRepository,
            QuizAnswerRepository quizAnswerRepository,
            ReviewService reviewService,
            ObjectMapper objectMapper
    ) {
        this.deckService = deckService;
        this.userService = userService;
        this.vocabItemRepository = vocabItemRepository;
        this.questionRepository = questionRepository;
        this.quizAttemptRepository = quizAttemptRepository;
        this.quizAnswerRepository = quizAnswerRepository;
        this.reviewService = reviewService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public QuizGenerateResponse generate(Authentication authentication, Long deckId) {
        User user = userService.currentUser(authentication);
        Deck deck = deckService.findOwnedDeck(user, deckId);
        List<VocabItem> items = vocabItemRepository.findAllByDeckIdAndDeckOwnerIdOrderByCreatedAtAsc(deckId, user.getId())
                .stream()
                .filter(item -> hasText(item.getMeaningVi()))
                .toList();

        if (items.size() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Need at least 2 vocabulary items with meanings to generate a quiz");
        }

        List<Question> generated = new ArrayList<>();
        List<VocabItem> shuffledItems = new ArrayList<>(items);
        Collections.shuffle(shuffledItems);

        java.util.Random random = new java.util.Random();
        for (VocabItem item : shuffledItems) {
            if (generated.size() >= QUESTION_LIMIT) {
                break;
            }
            int typeChoice = random.nextInt(4);
            switch (typeChoice) {
                case 0 -> generated.add(buildChooseMeaning(deck, user, item, items));
                case 1 -> generated.add(buildFillInBlank(deck, user, item));
                case 2 -> generated.add(buildTrueFalse(deck, user, item, items));
                case 3 -> generated.add(buildMatching(deck, user, item, items));
            }
        }

        List<Question> saved = questionRepository.saveAll(generated.stream().limit(QUESTION_LIMIT).toList());
        List<QuizQuestionResponse> questions = saved.stream().map(this::toQuestionResponse).toList();
        return new QuizGenerateResponse(deck.getId(), questions.size(), questions);
    }

    @Transactional
    public QuizAttemptResponse createAttempt(Authentication authentication, CreateQuizAttemptRequest request) {
        User user = userService.currentUser(authentication);
        Deck deck = deckService.findOwnedDeck(user, request.deckId());
        List<Question> questions = loadOwnedQuestions(user, request.questionIds());

        if (questions.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quiz attempt must include questions");
        }
        boolean hasForeignDeckQuestion = questions.stream().anyMatch(question -> !question.getDeck().getId().equals(deck.getId()));
        if (hasForeignDeckQuestion || questions.size() != new LinkedHashSet<>(request.questionIds()).size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid quiz questions");
        }

        QuizAttempt attempt = new QuizAttempt();
        attempt.setDeck(deck);
        attempt.setUser(user);
        attempt.setTotalQuestions(questions.size());
        QuizAttempt saved = quizAttemptRepository.save(attempt);

        return toAttemptResponse(saved, 0, questions);
    }

    @Transactional
    public QuizAnswerResponse answer(Authentication authentication, Long attemptId, AnswerQuizQuestionRequest request) {
        User user = userService.currentUser(authentication);
        QuizAttempt attempt = findOwnedAttempt(user, attemptId);
        Question question = questionRepository.findByIdAndOwnerId(request.questionId(), user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found"));

        if (!question.getDeck().getId().equals(attempt.getDeck().getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Question does not belong to this attempt deck");
        }
        if (quizAnswerRepository.findByAttemptIdAndQuestionId(attempt.getId(), question.getId()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Question already answered");
        }

        String submittedAnswer = request.answer() == null ? "" : request.answer().trim();
        boolean correct = isCorrect(question, submittedAnswer);

        QuizAnswer answer = new QuizAnswer();
        answer.setAttempt(attempt);
        answer.setQuestion(question);
        answer.setAnswer(submittedAnswer);
        answer.setCorrect(correct);
        QuizAnswer saved = quizAnswerRepository.save(answer);

        reviewService.applyQuizResult(user, question.getVocabItem(), correct, request.responseTimeMs());
        updateAttemptScore(attempt);

        return toAnswerResponse(saved);
    }

    @Transactional(readOnly = true)
    public QuizResultResponse result(Authentication authentication, Long attemptId) {
        User user = userService.currentUser(authentication);
        QuizAttempt attempt = findOwnedAttempt(user, attemptId);
        List<QuizAnswer> answers = quizAnswerRepository.findAllByAttemptIdOrderByIdAsc(attempt.getId());
        int answeredCount = answers.size();
        int scorePercent = attempt.getTotalQuestions() == 0 ? 0 : Math.round((attempt.getCorrectCount() * 100f) / attempt.getTotalQuestions());

        return new QuizResultResponse(
                attempt.getId(),
                attempt.getDeck().getId(),
                attempt.getTotalQuestions(),
                answeredCount,
                attempt.getCorrectCount(),
                scorePercent,
                attempt.getCompletedAt() != null,
                attempt.getCompletedAt(),
                answers.stream().map(this::toAnswerResponse).toList()
        );
    }

    private Question buildChooseMeaning(Deck deck, User user, VocabItem item, List<VocabItem> allItems) {
        List<String> options = new ArrayList<>();
        options.add(item.getMeaningVi());

        List<String> distractors = allItems.stream()
                .filter(candidate -> !candidate.getId().equals(item.getId()))
                .map(VocabItem::getMeaningVi)
                .filter(this::hasText)
                .distinct()
                .toList();
        List<String> shuffledDistractors = new ArrayList<>(distractors);
        Collections.shuffle(shuffledDistractors);
        options.addAll(shuffledDistractors.stream().limit(3).toList());
        Collections.shuffle(options);

        Question question = baseQuestion(deck, user, item, QuestionType.CHOOSE_MEANING);
        question.setPrompt("Choose the correct meaning of \"" + item.getWord() + "\".");
        question.setCorrectAnswer(item.getMeaningVi());
        question.setOptionsJson(writeOptions(options));
        question.setExplanation("\"" + item.getWord() + "\" means \"" + item.getMeaningVi() + "\".");
        return question;
    }

    private Question buildFillInBlank(Deck deck, User user, VocabItem item) {
        Question question = baseQuestion(deck, user, item, QuestionType.FILL_IN_BLANK);
        question.setPrompt(buildFillPrompt(item));
        question.setCorrectAnswer(item.getWord());
        question.setOptionsJson(null);
        question.setExplanation("The missing word is \"" + item.getWord() + "\". Meaning: " + item.getMeaningVi() + ".");
        return question;
    }

    private Question buildTrueFalse(Deck deck, User user, VocabItem item, List<VocabItem> allItems) {
        java.util.Random random = new java.util.Random();
        boolean isTrue = random.nextBoolean();
        String prompt;
        String correctAnswer;
        String explanation;
        
        if (isTrue) {
            prompt = "True or False: \"" + item.getWord() + "\" means \"" + item.getMeaningVi() + "\".";
            correctAnswer = "True";
            explanation = "Correct! \"" + item.getWord() + "\" indeed means \"" + item.getMeaningVi() + "\".";
        } else {
            List<VocabItem> distractors = allItems.stream()
                    .filter(candidate -> !candidate.getId().equals(item.getId()))
                    .toList();
            String wrongMeaning = distractors.isEmpty() ? "something else" : distractors.get(random.nextInt(distractors.size())).getMeaningVi();
            prompt = "True or False: \"" + item.getWord() + "\" means \"" + wrongMeaning + "\".";
            correctAnswer = "False";
            explanation = "False! \"" + item.getWord() + "\" actually means \"" + item.getMeaningVi() + "\" (not \"" + wrongMeaning + "\").";
        }

        Question question = baseQuestion(deck, user, item, QuestionType.TRUE_FALSE);
        question.setPrompt(prompt);
        question.setCorrectAnswer(correctAnswer);
        question.setOptionsJson(writeOptions(List.of("True", "False")));
        question.setExplanation(explanation);
        return question;
    }

    private Question buildMatching(Deck deck, User user, VocabItem item, List<VocabItem> allItems) {
        java.util.Random random = new java.util.Random();
        List<VocabItem> pool = new ArrayList<>(allItems);
        pool.removeIf(v -> v.getId().equals(item.getId()));
        Collections.shuffle(pool);
        
        List<VocabItem> matchingItems = new ArrayList<>();
        matchingItems.add(item);
        matchingItems.addAll(pool.stream().limit(4).toList());
        Collections.shuffle(matchingItems);

        List<String> words = matchingItems.stream().map(VocabItem::getWord).toList();
        List<String> meanings = new ArrayList<>(matchingItems.stream().map(VocabItem::getMeaningVi).toList());
        Collections.shuffle(meanings);

        String promptText = "Match the following words with their meanings.";
        
        Map<String, String> mapping = matchingItems.stream()
                .collect(Collectors.toMap(VocabItem::getWord, VocabItem::getMeaningVi));
        
        String correctAnswerJson;
        try {
            correctAnswerJson = objectMapper.writeValueAsString(mapping);
        } catch (Exception ex) {
            correctAnswerJson = "{}";
        }

        Map<String, List<String>> optionsMap = Map.of(
                "words", words,
                "meanings", meanings
        );
        String optionsJsonStr;
        try {
            optionsJsonStr = objectMapper.writeValueAsString(optionsMap);
        } catch (Exception ex) {
            optionsJsonStr = "{}";
        }

        Question question = baseQuestion(deck, user, item, QuestionType.MATCHING);
        question.setPrompt(promptText);
        question.setCorrectAnswer(correctAnswerJson);
        question.setOptionsJson(optionsJsonStr);
        question.setExplanation("Matching completed successfully.");
        return question;
    }

    private Question baseQuestion(Deck deck, User user, VocabItem item, QuestionType type) {
        Question question = new Question();
        question.setDeck(deck);
        question.setOwner(user);
        question.setVocabItem(item);
        question.setType(type);
        return question;
    }

    private String buildFillPrompt(VocabItem item) {
        if (hasText(item.getExampleEn())) {
            String word = item.getWord();
            String example = item.getExampleEn();
            String blanked = example.replaceAll("(?i)\\b" + java.util.regex.Pattern.quote(word) + "\\b", "____");
            if (!blanked.equals(example)) {
                return "Fill in the blank: " + blanked;
            }
        }
        return "Fill in the blank: The word meaning \"" + item.getMeaningVi() + "\" is ____.";
    }

    private List<Question> loadOwnedQuestions(User user, List<Long> questionIds) {
        Set<Long> uniqueIds = new LinkedHashSet<>(questionIds);
        List<Question> questions = questionRepository.findAllByIdInAndOwnerId(uniqueIds, user.getId());
        return uniqueIds.stream()
                .map(id -> questions.stream()
                        .filter(question -> question.getId().equals(id))
                        .findFirst()
                        .orElse(null))
                .filter(question -> question != null)
                .toList();
    }

    private void updateAttemptScore(QuizAttempt attempt) {
        int correctCount = quizAnswerRepository.countByAttemptIdAndCorrect(attempt.getId(), true);
        long answeredCount = quizAnswerRepository.countByAttemptId(attempt.getId());
        attempt.setCorrectCount(correctCount);
        if (answeredCount >= attempt.getTotalQuestions() && attempt.getCompletedAt() == null) {
            attempt.setCompletedAt(LocalDateTime.now());
        }
    }

    private QuizAttempt findOwnedAttempt(User user, Long attemptId) {
        return quizAttemptRepository.findByIdAndUserId(attemptId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz attempt not found"));
    }

    private boolean isCorrect(Question question, String answer) {
        if (question.getType() == QuestionType.FILL_IN_BLANK) {
            return normalizeForAnswer(answer).equals(normalizeForAnswer(question.getCorrectAnswer()));
        }
        if (question.getType() == QuestionType.MATCHING) {
            try {
                Map<String, String> userPairs = objectMapper.readValue(answer, new TypeReference<Map<String, String>>() {});
                Map<String, String> correctPairs = objectMapper.readValue(question.getCorrectAnswer(), new TypeReference<Map<String, String>>() {});
                if (userPairs.size() != correctPairs.size()) return false;
                for (Map.Entry<String, String> entry : correctPairs.entrySet()) {
                    String userVal = userPairs.get(entry.getKey());
                    if (userVal == null || !normalizeForAnswer(userVal).equals(normalizeForAnswer(entry.getValue()))) {
                        return false;
                    }
                }
                return true;
            } catch (Exception ex) {
                return false;
            }
        }
        return answer.trim().equalsIgnoreCase(question.getCorrectAnswer().trim());
    }

    private QuizAttemptResponse toAttemptResponse(QuizAttempt attempt, int answeredCount, List<Question> questions) {
        return new QuizAttemptResponse(
                attempt.getId(),
                attempt.getDeck().getId(),
                attempt.getTotalQuestions(),
                answeredCount,
                attempt.getCorrectCount(),
                attempt.getCompletedAt() != null,
                attempt.getCompletedAt(),
                attempt.getCreatedAt(),
                questions.stream().map(this::toQuestionResponse).toList()
        );
    }

    private QuizQuestionResponse toQuestionResponse(Question question) {
        return new QuizQuestionResponse(
                question.getId(),
                question.getDeck().getId(),
                question.getVocabItem().getId(),
                question.getType(),
                question.getPrompt(),
                readOptions(question),
                question.getExplanation(),
                question.getCreatedAt()
        );
    }

    private QuizAnswerResponse toAnswerResponse(QuizAnswer answer) {
        Question question = answer.getQuestion();
        return new QuizAnswerResponse(
                question.getId(),
                answer.getAnswer(),
                question.getCorrectAnswer(),
                answer.isCorrect(),
                question.getExplanation(),
                answer.getAnsweredAt()
        );
    }

    private String writeOptions(List<String> options) {
        try {
            return objectMapper.writeValueAsString(options);
        } catch (JsonProcessingException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not create quiz options");
        }
    }

    private Object readOptions(Question question) {
        String optionsJson = question.getOptionsJson();
        if (!hasText(optionsJson)) {
            return List.of();
        }
        try {
            if (question.getType() == QuestionType.MATCHING) {
                return objectMapper.readValue(optionsJson, MATCHING_OPTIONS);
            }
            return objectMapper.readValue(optionsJson, STRING_LIST);
        } catch (JsonProcessingException ex) {
            return List.of();
        }
    }

    private String normalizeForAnswer(String value) {
        String normalized = Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return normalized.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
