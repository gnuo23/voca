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
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
            int typeChoice = random.nextInt(5);
            switch (typeChoice) {
                case 0 -> generated.add(buildChooseMeaning(deck, user, item, items));
                case 1 -> generated.add(buildFillInBlank(deck, user, item));
                case 2 -> generated.add(buildClozeChoice(deck, user, item, items));
                case 3 -> generated.add(buildTrueFalse(deck, user, item, items));
                case 4 -> generated.add(buildMatching(deck, user, item, items));
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
    public QuizAttemptResponse createManualAttempt(Authentication authentication, Long deckId, ManualQuizAttemptRequest request) {
        User user = userService.currentUser(authentication);
        Deck deck = deckService.findOwnedDeck(user, deckId);
        List<VocabItem> deckItems = vocabItemRepository.findAllByDeckIdAndDeckOwnerIdOrderByCreatedAtAsc(deckId, user.getId())
                .stream()
                .filter(item -> hasText(item.getMeaningVi()))
                .toList();

        if (deckItems.size() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Need at least 2 vocabulary items with meanings to create a quiz");
        }

        ManualQuizAttemptRequest safeRequest = request == null
                ? new ManualQuizAttemptRequest(null, null, null, null)
                : request;
        List<Question> questions = hasManualQuestions(safeRequest)
                ? buildManualQuestions(deck, user, deckItems, safeRequest.questions())
                : buildAutoMappedQuestions(deck, user, deckItems, safeRequest);

        if (questions.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Manual quiz must include at least one question");
        }

        List<Question> savedQuestions = questionRepository.saveAll(questions);
        QuizAttempt attempt = new QuizAttempt();
        attempt.setDeck(deck);
        attempt.setUser(user);
        attempt.setTotalQuestions(savedQuestions.size());
        QuizAttempt savedAttempt = quizAttemptRepository.save(attempt);

        return toAttemptResponse(savedAttempt, 0, savedQuestions);
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

    private Question buildClozeChoice(Deck deck, User user, VocabItem item, List<VocabItem> allItems) {
        List<String> options = new ArrayList<>();
        options.add(item.getWord());

        List<String> distractors = allItems.stream()
                .filter(candidate -> !candidate.getId().equals(item.getId()))
                .map(VocabItem::getWord)
                .filter(this::hasText)
                .distinct()
                .collect(Collectors.toCollection(ArrayList::new));
        Collections.shuffle(distractors);
        options.addAll(distractors.stream().limit(3).toList());
        Collections.shuffle(options);

        Question question = baseQuestion(deck, user, item, QuestionType.CLOZE_CHOICE);
        question.setPrompt(buildClozePrompt(item));
        question.setCorrectAnswer(item.getWord());
        question.setOptionsJson(writeOptions(options));
        question.setExplanation("The correct word is \"" + item.getWord() + "\". Meaning: " + item.getMeaningVi() + ".");
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

    private List<Question> buildManualQuestions(
            Deck deck,
            User user,
            List<VocabItem> deckItems,
            List<ManualQuizAttemptRequest.ManualQuestionRequest> requestedQuestions
    ) {
        List<Question> questions = new ArrayList<>();
        Map<Long, VocabItem> byId = deckItems.stream().collect(Collectors.toMap(VocabItem::getId, item -> item));
        Map<String, VocabItem> byWord = vocabByNormalizedWord(deckItems);

        for (ManualQuizAttemptRequest.ManualQuestionRequest request : requestedQuestions) {
            if (request == null) {
                continue;
            }
            QuestionType type = request.type();
            if (type == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Manual question type is required");
            }
            VocabItem vocabItem = resolveManualQuestionVocab(request, byId, byWord);
            Question question = baseQuestion(deck, user, vocabItem, type);
            question.setPrompt(cleanRequired(request.prompt(), "Manual question prompt is required", 1000));
            question.setExplanation(cleanOptional(request.explanation(), 1500));

            if (type == QuestionType.MATCHING) {
                Map<String, String> correctPairs = manualCorrectPairs(request);
                if (correctPairs.isEmpty()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Matching questions require correctPairs");
                }
                question.setCorrectAnswer(writeJson(correctPairs, 1000, "Matching answer is too large"));
                question.setOptionsJson(writeJson(manualMatchingOptions(request, correctPairs), 4000, "Matching options are too large"));
            } else {
                question.setCorrectAnswer(cleanRequired(request.correctAnswer(), "Manual question correctAnswer is required", 1000));
                question.setOptionsJson(manualOptionsJson(type, request.options()));
            }
            questions.add(question);
        }

        return questions;
    }

    private List<Question> buildAutoMappedQuestions(
            Deck deck,
            User user,
            List<VocabItem> deckItems,
            ManualQuizAttemptRequest request
    ) {
        List<QuizTerm> terms = resolveQuizTerms(deckItems, request.vocabPairs());
        if (terms.size() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Manual quiz needs at least 2 mapped vocabulary pairs");
        }

        List<QuestionType> requestedTypes = normalizeQuestionTypes(request.questionTypes());
        int requestedLimit = request.limit() == null ? terms.size() : Math.max(1, request.limit());
        int questionCount = Math.min(requestedLimit, terms.size());
        List<Question> questions = new ArrayList<>();

        for (int i = 0; i < questionCount; i++) {
            QuizTerm term = terms.get(i);
            QuestionType type = requestedTypes.get(i % requestedTypes.size());
            questions.add(switch (type) {
                case CHOOSE_MEANING -> buildChooseMeaning(deck, user, term, terms);
                case FILL_IN_BLANK -> buildFillInBlank(deck, user, term);
                case CLOZE_CHOICE -> buildClozeChoice(deck, user, term, terms);
                case TRUE_FALSE -> buildTrueFalse(deck, user, term, terms, i);
                case MATCHING -> buildMatching(deck, user, term, terms, i);
            });
        }

        return questions;
    }

    private Question buildChooseMeaning(Deck deck, User user, QuizTerm term, List<QuizTerm> allTerms) {
        List<String> distractors = allTerms.stream()
                .filter(candidate -> !candidate.vocabItem().getId().equals(term.vocabItem().getId()))
                .map(QuizTerm::meaning)
                .filter(this::hasText)
                .distinct()
                .collect(Collectors.toCollection(ArrayList::new));
        Collections.shuffle(distractors);

        List<String> options = new ArrayList<>();
        options.add(term.meaning());
        options.addAll(distractors.stream().limit(3).toList());
        Collections.shuffle(options);

        Question question = baseQuestion(deck, user, term.vocabItem(), QuestionType.CHOOSE_MEANING);
        question.setPrompt("Choose the correct meaning of \"" + term.word() + "\".");
        question.setCorrectAnswer(term.meaning());
        question.setOptionsJson(writeOptions(options));
        question.setExplanation("\"" + term.word() + "\" means \"" + term.meaning() + "\".");
        return question;
    }

    private Question buildFillInBlank(Deck deck, User user, QuizTerm term) {
        Question question = baseQuestion(deck, user, term.vocabItem(), QuestionType.FILL_IN_BLANK);
        question.setPrompt("Fill in the blank: The word meaning \"" + term.meaning() + "\" is ____.");
        question.setCorrectAnswer(term.word());
        question.setOptionsJson(null);
        question.setExplanation("The missing word is \"" + term.word() + "\". Meaning: " + term.meaning() + ".");
        return question;
    }

    private Question buildClozeChoice(Deck deck, User user, QuizTerm term, List<QuizTerm> allTerms) {
        List<String> distractors = allTerms.stream()
                .filter(candidate -> !candidate.vocabItem().getId().equals(term.vocabItem().getId()))
                .map(QuizTerm::word)
                .filter(this::hasText)
                .distinct()
                .collect(Collectors.toCollection(ArrayList::new));
        Collections.shuffle(distractors);

        List<String> options = new ArrayList<>();
        options.add(term.word());
        options.addAll(distractors.stream().limit(3).toList());
        Collections.shuffle(options);

        Question question = baseQuestion(deck, user, term.vocabItem(), QuestionType.CLOZE_CHOICE);
        question.setPrompt("Choose the word that completes the sentence: ____ means \"" + term.meaning() + "\".");
        question.setCorrectAnswer(term.word());
        question.setOptionsJson(writeOptions(options));
        question.setExplanation("The correct word is \"" + term.word() + "\". Meaning: " + term.meaning() + ".");
        return question;
    }

    private Question buildTrueFalse(Deck deck, User user, QuizTerm term, List<QuizTerm> allTerms, int index) {
        boolean isTrue = index % 2 == 0;
        String shownMeaning = term.meaning();
        if (!isTrue) {
            shownMeaning = allTerms.stream()
                    .filter(candidate -> !candidate.vocabItem().getId().equals(term.vocabItem().getId()))
                    .map(QuizTerm::meaning)
                    .findFirst()
                    .orElse(term.meaning());
        }

        Question question = baseQuestion(deck, user, term.vocabItem(), QuestionType.TRUE_FALSE);
        question.setPrompt("True or False: \"" + term.word() + "\" means \"" + shownMeaning + "\".");
        question.setCorrectAnswer(isTrue ? "True" : "False");
        question.setOptionsJson(writeOptions(List.of("True", "False")));
        question.setExplanation(isTrue
                ? "Correct! \"" + term.word() + "\" means \"" + term.meaning() + "\"."
                : "False! \"" + term.word() + "\" means \"" + term.meaning() + "\".");
        return question;
    }

    private Question buildMatching(Deck deck, User user, QuizTerm term, List<QuizTerm> allTerms, int index) {
        List<QuizTerm> pool = new ArrayList<>(allTerms);
        Collections.rotate(pool, -index);
        List<QuizTerm> matchingTerms = pool.stream().limit(Math.min(5, pool.size())).toList();

        List<String> words = matchingTerms.stream().map(QuizTerm::word).toList();
        List<String> meanings = new ArrayList<>(matchingTerms.stream().map(QuizTerm::meaning).toList());
        Collections.shuffle(meanings);
        Map<String, String> correctPairs = matchingTerms.stream()
                .collect(Collectors.toMap(QuizTerm::word, QuizTerm::meaning, (left, right) -> left, LinkedHashMap::new));

        Question question = baseQuestion(deck, user, term.vocabItem(), QuestionType.MATCHING);
        question.setPrompt("Match the following words with their meanings.");
        question.setCorrectAnswer(writeJson(correctPairs, 1000, "Matching answer is too large"));
        question.setOptionsJson(writeJson(Map.of("words", words, "meanings", meanings), 4000, "Matching options are too large"));
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

    private String buildClozePrompt(VocabItem item) {
        if (hasText(item.getExampleEn())) {
            String word = item.getWord();
            String example = item.getExampleEn();
            String blanked = example.replaceAll("(?i)\\b" + java.util.regex.Pattern.quote(word) + "\\b", "____");
            if (!blanked.equals(example)) {
                return "Choose the word that completes the sentence: " + blanked;
            }
        }
        return "Choose the word that completes the sentence: ____ means \"" + item.getMeaningVi() + "\".";
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
        return writeJson(options, 4000, "Could not create quiz options");
    }

    private String writeJson(Object value, int maxLength, String tooLargeMessage) {
        try {
            String json = objectMapper.writeValueAsString(value);
            if (json.length() > maxLength) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, tooLargeMessage);
            }
            return json;
        } catch (JsonProcessingException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not create quiz options");
        }
    }

    private boolean hasManualQuestions(ManualQuizAttemptRequest request) {
        return request.questions() != null && !request.questions().isEmpty();
    }

    private List<QuestionType> normalizeQuestionTypes(List<QuestionType> questionTypes) {
        if (questionTypes == null || questionTypes.isEmpty()) {
            return List.of(QuestionType.CLOZE_CHOICE, QuestionType.CHOOSE_MEANING, QuestionType.TRUE_FALSE, QuestionType.MATCHING);
        }
        EnumSet<QuestionType> seen = EnumSet.noneOf(QuestionType.class);
        List<QuestionType> normalized = questionTypes.stream()
                .filter(type -> type != null && seen.add(type))
                .toList();
        return normalized.isEmpty()
                ? List.of(QuestionType.CLOZE_CHOICE, QuestionType.CHOOSE_MEANING, QuestionType.TRUE_FALSE, QuestionType.MATCHING)
                : normalized;
    }

    private List<QuizTerm> resolveQuizTerms(List<VocabItem> deckItems, List<ManualQuizAttemptRequest.VocabPairRequest> requestedPairs) {
        Map<Long, VocabItem> byId = deckItems.stream().collect(Collectors.toMap(VocabItem::getId, item -> item));
        Map<String, VocabItem> byWord = vocabByNormalizedWord(deckItems);

        if (requestedPairs == null || requestedPairs.isEmpty()) {
            return deckItems.stream()
                    .map(item -> new QuizTerm(item, item.getWord(), item.getMeaningVi()))
                    .toList();
        }

        List<QuizTerm> terms = new ArrayList<>();
        Set<Long> seenVocabIds = new LinkedHashSet<>();
        for (ManualQuizAttemptRequest.VocabPairRequest pair : requestedPairs) {
            if (pair == null) {
                continue;
            }
            VocabItem item = resolveVocabPair(pair, byId, byWord);
            if (!seenVocabIds.add(item.getId())) {
                continue;
            }
            String meaning = hasText(pair.meaning()) ? pair.meaning().trim().replaceAll("\\s+", " ") : item.getMeaningVi();
            terms.add(new QuizTerm(item, item.getWord(), meaning));
        }
        return terms;
    }

    private VocabItem resolveVocabPair(
            ManualQuizAttemptRequest.VocabPairRequest pair,
            Map<Long, VocabItem> byId,
            Map<String, VocabItem> byWord
    ) {
        if (pair.vocabId() != null) {
            VocabItem item = byId.get(pair.vocabId());
            if (item == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown vocabId in manual quiz: " + pair.vocabId());
            }
            return item;
        }
        if (hasText(pair.word())) {
            VocabItem item = byWord.get(normalizeWord(pair.word()));
            if (item == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Manual quiz word is not in this deck: " + pair.word());
            }
            return item;
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Manual vocabPairs require vocabId or word");
    }

    private VocabItem resolveManualQuestionVocab(
            ManualQuizAttemptRequest.ManualQuestionRequest request,
            Map<Long, VocabItem> byId,
            Map<String, VocabItem> byWord
    ) {
        if (request.vocabId() != null) {
            VocabItem item = byId.get(request.vocabId());
            if (item == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown vocabId in manual question: " + request.vocabId());
            }
            return item;
        }
        if (hasText(request.word())) {
            VocabItem item = byWord.get(normalizeWord(request.word()));
            if (item != null) {
                return item;
            }
        }
        if (request.correctPairs() != null && !request.correctPairs().isEmpty()) {
            String firstWord = request.correctPairs().keySet().iterator().next();
            VocabItem item = byWord.get(normalizeWord(firstWord));
            if (item != null) {
                return item;
            }
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Manual questions require vocabId or word from this deck");
    }

    private Map<String, String> manualCorrectPairs(ManualQuizAttemptRequest.ManualQuestionRequest request) {
        if (request.correctPairs() != null && !request.correctPairs().isEmpty()) {
            Map<String, String> pairs = new LinkedHashMap<>();
            request.correctPairs().forEach((word, meaning) -> {
                if (hasText(word) && hasText(meaning)) {
                    pairs.put(word.trim().replaceAll("\\s+", " "), meaning.trim().replaceAll("\\s+", " "));
                }
            });
            return pairs;
        }
        if (hasText(request.correctAnswer())) {
            try {
                return objectMapper.readValue(request.correctAnswer(), new TypeReference<Map<String, String>>() {
                });
            } catch (JsonProcessingException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Matching correctAnswer must be JSON object");
            }
        }
        return Map.of();
    }

    private Map<String, List<String>> manualMatchingOptions(
            ManualQuizAttemptRequest.ManualQuestionRequest request,
            Map<String, String> correctPairs
    ) {
        if (request.matchingOptions() != null
                && request.matchingOptions().get("words") != null
                && request.matchingOptions().get("meanings") != null) {
            return request.matchingOptions();
        }
        List<String> words = new ArrayList<>(correctPairs.keySet());
        List<String> meanings = new ArrayList<>(correctPairs.values());
        Collections.shuffle(meanings);
        return Map.of("words", words, "meanings", meanings);
    }

    private String manualOptionsJson(QuestionType type, List<String> options) {
        if (type == QuestionType.FILL_IN_BLANK) {
            return null;
        }
        List<String> cleanedOptions = options == null ? List.of() : options.stream()
                .filter(this::hasText)
                .map(value -> value.trim().replaceAll("\\s+", " "))
                .distinct()
                .toList();
        if (type == QuestionType.TRUE_FALSE && cleanedOptions.isEmpty()) {
            cleanedOptions = List.of("True", "False");
        }
        if ((type == QuestionType.CHOOSE_MEANING || type == QuestionType.CLOZE_CHOICE) && cleanedOptions.size() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choice questions require at least 2 options");
        }
        return writeOptions(cleanedOptions);
    }

    private Map<String, VocabItem> vocabByNormalizedWord(List<VocabItem> items) {
        Map<String, VocabItem> byWord = new HashMap<>();
        for (VocabItem item : items) {
            byWord.putIfAbsent(normalizeWord(item.getWord()), item);
        }
        return byWord;
    }

    private String cleanRequired(String value, String message, int maxLength) {
        if (!hasText(value)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return cleanOptional(value, maxLength);
    }

    private String cleanOptional(String value, int maxLength) {
        if (!hasText(value)) {
            return null;
        }
        String cleaned = value.trim().replaceAll("\\s+", " ");
        if (cleaned.length() > maxLength) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Manual quiz text is too long");
        }
        return cleaned;
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

    private String normalizeWord(String word) {
        return word == null ? "" : word.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private record QuizTerm(VocabItem vocabItem, String word, String meaning) {
    }
}
