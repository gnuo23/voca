package com.voca.backend.learn;

import com.voca.backend.deck.Deck;
import com.voca.backend.deck.DeckService;
import com.voca.backend.review.ReviewService;
import com.voca.backend.user.User;
import com.voca.backend.user.UserService;
import com.voca.backend.vocab.UserProgress;
import com.voca.backend.vocab.UserProgressRepository;
import com.voca.backend.vocab.VocabItem;
import com.voca.backend.vocab.VocabItemRepository;
import com.voca.backend.vocab.VocabProgressStatus;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class LearnService {

    private static final int INTRO_BATCH_SIZE = 6;
    private static final String DEFAULT_QUESTION_TYPES = "MCQ,WRITTEN";

    private final LearnSessionRepository sessionRepo;
    private final LearnSessionItemRepository sessionItemRepo;
    private final LearnAnswerRepository answerRepo;
    private final VocabItemRepository vocabRepo;
    private final UserProgressRepository progressRepo;
    private final DeckService deckService;
    private final UserService userService;
    private final ReviewService reviewService;
    private final Random rng = new Random();

    public LearnService(
            LearnSessionRepository sessionRepo,
            LearnSessionItemRepository sessionItemRepo,
            LearnAnswerRepository answerRepo,
            VocabItemRepository vocabRepo,
            UserProgressRepository progressRepo,
            DeckService deckService,
            UserService userService,
            ReviewService reviewService
    ) {
        this.sessionRepo = sessionRepo;
        this.sessionItemRepo = sessionItemRepo;
        this.answerRepo = answerRepo;
        this.vocabRepo = vocabRepo;
        this.progressRepo = progressRepo;
        this.deckService = deckService;
        this.userService = userService;
        this.reviewService = reviewService;
    }

    @Transactional
    public LearnSessionResponse startSession(Authentication auth, StartLearnSessionRequest request) {
        User user = userService.currentUser(auth);
        Deck deck = deckService.findOwnedDeck(user, request.deckId());

        sessionRepo.findFirstByUserIdAndDeckIdAndStatusOrderByCreatedAtDesc(
                user.getId(), deck.getId(), LearnSessionStatus.IN_PROGRESS
        ).ifPresent(session -> {
            session.setStatus(LearnSessionStatus.ABANDONED);
            sessionRepo.save(session);
        });

        List<VocabItem> allVocabs = vocabRepo.findAllByDeckIdAndDeckOwnerIdOrderByCreatedAtAsc(deck.getId(), user.getId())
                .stream()
                .filter(v -> hasText(v.getMeaningVi()))
                .toList();

        if (allVocabs.size() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Need at least 2 vocabulary items with meanings to learn");
        }

        LearnSessionScope scope = request.scope() != null ? request.scope() : LearnSessionScope.NOT_MASTERED;
        LearnGoal goal = request.goal() != null ? request.goal() : LearnGoal.MASTER_ALL;
        LearnAnswerDirection answerDirection = request.answerDirection() != null ? request.answerDirection() : LearnAnswerDirection.BOTH;
        LearnGradingMode gradingMode = request.gradingMode() != null ? request.gradingMode() : LearnGradingMode.ACCENT_INSENSITIVE;

        List<VocabItem> targetVocabs = filterByScope(user, allVocabs, scope);
        if (targetVocabs.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No vocabulary items match the chosen scope");
        }

        LearnSession session = new LearnSession();
        session.setUser(user);
        session.setDeck(deck);
        session.setScope(scope);
        session.setGoal(goal);
        session.setAnswerDirection(answerDirection);
        session.setGradingMode(gradingMode);
        session.setEnabledQuestionTypes(serializeQuestionTypes(request.questionTypes()));
        session.setTotalTerms(targetVocabs.size());
        session.setMasteredTerms(0);
        session.setStatus(LearnSessionStatus.IN_PROGRESS);
        sessionRepo.save(session);

        List<VocabItem> mixedVocabs = new ArrayList<>(targetVocabs);
        Collections.shuffle(mixedVocabs, rng);

        List<LearnSessionItem> sessionItems = new ArrayList<>();
        for (int i = 0; i < mixedVocabs.size(); i++) {
            LearnSessionItem item = new LearnSessionItem();
            item.setSession(session);
            item.setVocabItem(mixedVocabs.get(i));
            item.setStage(LearnItemStage.NEW);
            item.setCorrectStreak(0);
            item.setTotalAttempts(0);
            item.setCorrectAttempts(0);
            item.setIncorrectAttempts(0);
            item.setPriority(mixedVocabs.size() - i);
            sessionItems.add(item);
        }
        sessionItemRepo.saveAll(sessionItems);

        return LearnSessionResponse.from(session);
    }

    @Transactional
    public LearnQuestionResponse getNextQuestion(Authentication auth, Long sessionId) {
        User user = userService.currentUser(auth);
        LearnSession session = sessionRepo.findByIdAndUserId(sessionId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Learn session not found"));

        if (session.getStatus() != LearnSessionStatus.IN_PROGRESS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Learn session is not active");
        }

        List<LearnSessionItem> items = sessionItemRepo.findAllBySessionId(session.getId());
        normalizeItemStages(items);

        LearnQuestionResponse.Progress progress = progressFor(session, items);
        if (progress.remainingTerms() == 0) {
            completeSession(session, progress);
            return new LearnQuestionResponse(
                    null, null, null, null, null, "Session complete!", null, null, null, null, null,
                    progress
            );
        }

        LearnSessionItem currentItem = selectNextItem(session, items);
        List<VocabItem> allDeckItems = vocabRepo.findAllByDeckIdAndDeckOwnerIdOrderByCreatedAtAsc(session.getDeck().getId(), user.getId())
                .stream()
                .filter(v -> hasText(v.getMeaningVi()))
                .toList();
        GeneratedLearnQuestion generated = generateQuestion(session, currentItem, allDeckItems);

        return new LearnQuestionResponse(
                currentItem.getId(),
                currentItem.getVocabItem().getId(),
                currentItem.getVocabItem().getWord(),
                generated.type(),
                questionToken(session, currentItem, generated),
                generated.prompt(),
                generated.options(),
                generated.trueFalseStatement(),
                answerHint(generated),
                normalizeStage(currentItem.getStage()),
                buildVocabContext(currentItem.getVocabItem()),
                progress
        );
    }

    @Transactional
    public LearnAnswerResponse submitAnswer(Authentication auth, Long sessionId, SubmitLearnAnswerRequest request) {
        User user = userService.currentUser(auth);
        LearnSession session = sessionRepo.findByIdAndUserId(sessionId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Learn session not found"));

        if (session.getStatus() != LearnSessionStatus.IN_PROGRESS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Learn session is not active");
        }

        LearnSessionItem item = sessionItemRepo.findById(request.sessionItemId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session item not found"));

        if (!item.getSession().getId().equals(session.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Item does not belong to this session");
        }

        List<LearnSessionItem> items = sessionItemRepo.findAllBySessionId(session.getId());
        normalizeItemStages(items);
        if (isFinishedItem(session, item)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Item is already complete");
        }

        List<VocabItem> allDeckItems = vocabRepo.findAllByDeckIdAndDeckOwnerIdOrderByCreatedAtAsc(session.getDeck().getId(), user.getId())
                .stream()
                .filter(v -> hasText(v.getMeaningVi()))
                .toList();
        GeneratedLearnQuestion generated = generateQuestion(session, item, allDeckItems);

        LearnQuestionType submittedType = request.questionType() == null ? generated.type() : request.questionType();
        if (submittedType != generated.type()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Answer question type does not match the active prompt");
        }
        if (hasText(request.questionToken())) {
            String expectedToken = questionToken(session, item, generated);
            if (!expectedToken.equals(request.questionToken())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Question is no longer active");
            }
        }

        GradeResult grade = gradeAnswer(request.answer(), generated.correctAnswer(), generated.type(), session.getGradingMode());
        boolean correct = grade.verdict() == GradeVerdict.CORRECT;
        LearnItemStage stageBefore = normalizeStage(item.getStage());

        LearnAnswer answer = new LearnAnswer();
        answer.setSession(session);
        answer.setSessionItem(item);
        answer.setQuestionType(generated.type());
        answer.setPrompt(generated.trueFalseStatement() != null ? generated.trueFalseStatement() : generated.prompt());
        answer.setUserAnswer(request.answer());
        answer.setCorrectAnswer(generated.correctAnswer());
        answer.setCorrect(correct);
        answer.setVerdict(grade.verdict());
        answer.setSimilarityScore(grade.similarity());
        answer.setStageBefore(stageBefore);
        answer.setResponseTimeMs(request.responseTimeMs());
        captureReviewSnapshot(answer, user, item.getVocabItem());
        answerRepo.save(answer);

        item.incrementTotalAttempts();
        item.setLastAnsweredAt(LocalDateTime.now());

        if (correct) {
            item.incrementCorrectAttempts();
            item.setCorrectStreak(item.getCorrectStreak() + 1);
            item.setStage(nextCorrectStage(session, item, generated.type()));
            item.setPriority(Math.max(0, item.getPriority() - 2));
        } else {
            item.incrementIncorrectAttempts();
            item.setCorrectStreak(0);
            item.setStage(nextIncorrectStage(item.getStage()));
            item.setPriority(item.getPriority() + 5);
        }
        sessionItemRepo.save(item);

        session.incrementTotalAnswers();
        if (correct) {
            session.incrementCorrectAnswers();
        }

        reviewService.applyQuizResult(
                user,
                item.getVocabItem(),
                correct,
                request.responseTimeMs() == null ? null : request.responseTimeMs().intValue(),
                request.quality()
        );

        items = sessionItemRepo.findAllBySessionId(session.getId());
        LearnQuestionResponse.Progress progress = progressFor(session, items);
        if (progress.remainingTerms() == 0) {
            completeSession(session, progress);
        } else {
            session.setMasteredTerms(progress.masteredTerms());
        }
        sessionRepo.save(session);

        return answerResponse(answer, item, progress);
    }

    @Transactional
    public LearnAnswerResponse overrideAnswer(Authentication auth, Long sessionId, OverrideLearnAnswerRequest request) {
        User user = userService.currentUser(auth);
        LearnSession session = sessionRepo.findByIdAndUserId(sessionId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Learn session not found"));

        if (session.getStatus() != LearnSessionStatus.IN_PROGRESS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Learn session is not active");
        }

        LearnSessionItem item = sessionItemRepo.findById(request.sessionItemId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session item not found"));

        if (!item.getSession().getId().equals(session.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Item does not belong to this session");
        }

        LearnAnswer answer = answerRepo.findFirstBySessionIdAndSessionItemIdOrderByAnsweredAtDesc(session.getId(), item.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Answer not found"));

        if (request.targetVerdict() != GradeVerdict.CORRECT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only overriding to correct is supported");
        }

        if (answer.isCorrect()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only incorrect answers can be overridden");
        }

        LearnItemStage stageBefore = answer.getStageBefore() == null
                ? inferStageBeforeOverride(item.getStage())
                : normalizeStage(answer.getStageBefore());

        answer.setCorrect(true);
        answer.setVerdict(GradeVerdict.CORRECT);
        answerRepo.save(answer);

        item.setStage(stageBefore);
        item.incrementCorrectAttempts();
        item.setIncorrectAttempts(Math.max(0, item.getIncorrectAttempts() - 1));
        item.setCorrectStreak(item.getCorrectStreak() + 1);
        item.setStage(nextCorrectStage(session, item, answer.getQuestionType()));
        item.setPriority(Math.max(0, item.getPriority() - 7));
        sessionItemRepo.save(item);

        replayReviewAfterOverride(user, answer);
        session.incrementCorrectAnswers();

        List<LearnSessionItem> items = sessionItemRepo.findAllBySessionId(session.getId());
        LearnQuestionResponse.Progress progress = progressFor(session, items);
        if (progress.remainingTerms() == 0) {
            completeSession(session, progress);
        } else {
            session.setMasteredTerms(progress.masteredTerms());
        }
        sessionRepo.save(session);

        return answerResponse(answer, item, progress);
    }

    @Transactional(readOnly = true)
    public LearnSessionResultResponse getSessionResult(Authentication auth, Long sessionId) {
        User user = userService.currentUser(auth);
        LearnSession session = sessionRepo.findByIdAndUserId(sessionId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Learn session not found"));

        List<LearnSessionItem> items = sessionItemRepo.findAllBySessionId(session.getId());
        List<LearnAnswer> answers = answerRepo.findAllBySessionIdOrderByAnsweredAtAsc(session.getId());

        List<LearnSessionResultResponse.ItemSummary> itemSummaries = items.stream()
                .map(item -> new LearnSessionResultResponse.ItemSummary(
                        item.getVocabItem().getId(),
                        item.getVocabItem().getWord(),
                        item.getVocabItem().getPartOfSpeech(),
                        item.getVocabItem().getMeaningVi(),
                        normalizeStage(item.getStage()),
                        item.getCorrectAttempts(),
                        item.getIncorrectAttempts(),
                        item.getTotalAttempts()
                ))
                .toList();

        List<LearnSessionResultResponse.AnswerSummary> answerSummaries = answers.stream()
                .map(ans -> new LearnSessionResultResponse.AnswerSummary(
                        ans.getQuestionType(),
                        ans.getPrompt(),
                        ans.getUserAnswer(),
                        ans.getCorrectAnswer(),
                        ans.isCorrect(),
                        ans.getVerdict(),
                        ans.getSimilarityScore(),
                        ans.getResponseTimeMs(),
                        ans.getAnsweredAt()
                ))
                .toList();

        return new LearnSessionResultResponse(
                LearnSessionResponse.from(session),
                itemSummaries,
                answerSummaries
        );
    }

    private List<VocabItem> filterByScope(User user, List<VocabItem> allVocabs, LearnSessionScope scope) {
        if (scope == LearnSessionScope.ALL) {
            return allVocabs;
        }

        Map<Long, UserProgress> progressMap = progressRepo.findAllByUserIdAndVocabItemIdIn(
                user.getId(), allVocabs.stream().map(VocabItem::getId).toList()
        ).stream().collect(Collectors.toMap(p -> p.getVocabItem().getId(), Function.identity()));

        if (scope == LearnSessionScope.NOT_MASTERED) {
            return allVocabs.stream()
                    .filter(v -> {
                        UserProgress p = progressMap.get(v.getId());
                        return p == null || p.getStatus() != VocabProgressStatus.MASTERED;
                    })
                    .toList();
        }
        if (scope == LearnSessionScope.DIFFICULT_ONLY) {
            return allVocabs.stream()
                    .filter(v -> {
                        UserProgress p = progressMap.get(v.getId());
                        return p != null && p.getStatus() == VocabProgressStatus.DIFFICULT;
                    })
                    .toList();
        }
        if (scope == LearnSessionScope.NEW_ONLY) {
            return allVocabs.stream()
                    .filter(v -> {
                        UserProgress p = progressMap.get(v.getId());
                        return p == null || p.getStatus() == VocabProgressStatus.NEW;
                    })
                    .toList();
        }
        return allVocabs;
    }

    private LearnSessionItem selectNextItem(LearnSession session, List<LearnSessionItem> items) {
        List<LearnSessionItem> candidates = items.stream()
                .filter(item -> !isFinishedItem(session, item))
                .toList();

        long activeIntroducedCount = candidates.stream()
                .filter(item -> normalizeStage(item.getStage()) != LearnItemStage.NEW)
                .count();
        boolean hasIntroducedCandidates = candidates.stream()
                .anyMatch(item -> normalizeStage(item.getStage()) != LearnItemStage.NEW);
        boolean canIntroduceNew = activeIntroducedCount < INTRO_BATCH_SIZE || !hasIntroducedCandidates;

        List<LocalDateTime> recentAnswerTimes = candidates.stream()
                .map(LearnSessionItem::getLastAnsweredAt)
                .filter(value -> value != null)
                .distinct()
                .sorted((a, b) -> b.compareTo(a))
                .limit(2)
                .toList();

        List<LearnSessionItem> eligible = candidates.stream()
                .filter(item -> canIntroduceNew || normalizeStage(item.getStage()) != LearnItemStage.NEW)
                .filter(item -> candidates.size() <= 2 || item.getLastAnsweredAt() == null || !recentAnswerTimes.contains(item.getLastAnsweredAt()))
                .toList();

        if (eligible.isEmpty()) {
            eligible = candidates;
        }

        return eligible.stream()
                .max((left, right) -> Integer.compare(selectionScore(left), selectionScore(right)))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "No learn items remain"));
    }

    private int selectionScore(LearnSessionItem item) {
        LearnItemStage stage = normalizeStage(item.getStage());
        int stageWeight = switch (stage) {
            case FAMILIAR -> 10;
            case LEARNING -> 8;
            case SEEN -> 5;
            case NEW -> 1;
            case MASTERED, NOT_STUDIED, STILL_LEARNING -> 0;
        };
        int errorBoost = item.getIncorrectAttempts() >= 2 ? item.getIncorrectAttempts() * 5 : item.getIncorrectAttempts() * 3;
        return item.getPriority() + stageWeight + errorBoost - item.getCorrectStreak();
    }

    private GeneratedLearnQuestion generateQuestion(LearnSession session, LearnSessionItem item, List<VocabItem> allDeckItems) {
        LearnQuestionType type = selectQuestionType(session, item);
        LearnAnswerDirection direction = selectAnswerDirection(session, item);
        VocabItem vocab = item.getVocabItem();

        if (type == LearnQuestionType.TRUE_FALSE) {
            TrueFalseQuestion trueFalse = buildTrueFalseQuestion(session, item, direction, allDeckItems);
            return new GeneratedLearnQuestion(
                    type,
                    "Is this statement True or False?",
                    List.of("True", "False"),
                    trueFalse.statement(),
                    trueFalse.correctAnswer()
            );
        }

        if (type == LearnQuestionType.MCQ) {
            boolean wordToMeaning = direction == LearnAnswerDirection.WORD_TO_MEANING;
            String prompt = wordToMeaning
                    ? "Choose the correct meaning of \"" + vocab.getWord() + "\"."
                    : "Choose the word for this meaning: \"" + vocab.getMeaningVi() + "\".";
            String correctAnswer = wordToMeaning ? vocab.getMeaningVi() : vocab.getWord();
            List<String> options = buildMcqOptions(session, item, allDeckItems, wordToMeaning);
            return new GeneratedLearnQuestion(type, prompt, options, null, correctAnswer);
        }

        boolean wordToMeaning = direction == LearnAnswerDirection.WORD_TO_MEANING;
        String prompt = wordToMeaning
                ? "Type the meaning of \"" + vocab.getWord() + "\"."
                : "Type the word for this meaning: \"" + vocab.getMeaningVi() + "\".";
        String correctAnswer = wordToMeaning ? vocab.getMeaningVi() : vocab.getWord();
        return new GeneratedLearnQuestion(type, prompt, null, null, correctAnswer);
    }

    private List<String> buildMcqOptions(LearnSession session, LearnSessionItem item, List<VocabItem> allDeckItems, boolean wordToMeaning) {
        VocabItem vocab = item.getVocabItem();
        String correctAnswer = wordToMeaning ? vocab.getMeaningVi() : vocab.getWord();

        List<String> distractors = allDeckItems.stream()
                .filter(v -> !v.getId().equals(vocab.getId()))
                .map(v -> wordToMeaning ? v.getMeaningVi() : v.getWord())
                .filter(this::hasText)
                .distinct()
                .collect(Collectors.toCollection(ArrayList::new));
        Collections.shuffle(distractors, seededQuestionRandom(session.getId(), item));

        List<String> options = new ArrayList<>();
        options.add(correctAnswer);
        options.addAll(distractors.stream().limit(3).toList());
        Collections.shuffle(options, seededQuestionRandom(session.getId() + 7, item));
        return options;
    }

    private LearnQuestionType selectQuestionType(LearnSession session, LearnSessionItem item) {
        Set<LearnQuestionType> enabled = enabledQuestionTypes(session);
        LearnItemStage stage = normalizeStage(item.getStage());
        // Step 1 (NEW): MCQ for recognition
        if (stage == LearnItemStage.NEW || stage == LearnItemStage.SEEN) {
            return seededChoice(session, item, enabled, LearnQuestionType.MCQ, LearnQuestionType.TRUE_FALSE, LearnQuestionType.WRITTEN);
        }
        // Steps 2 & 3 (LEARNING, FAMILIAR): Written for recall
        return preferWritten(enabled);
    }

    private LearnQuestionType seededChoice(
            LearnSession session,
            LearnSessionItem item,
            Set<LearnQuestionType> enabled,
            LearnQuestionType first,
            LearnQuestionType second,
            LearnQuestionType fallback
    ) {
        List<LearnQuestionType> choices = new ArrayList<>();
        if (enabled.contains(first)) {
            choices.add(first);
        }
        if (enabled.contains(second)) {
            choices.add(second);
        }
        if (enabled.contains(fallback)) {
            choices.add(fallback);
        }
        if (choices.isEmpty()) {
            return LearnQuestionType.MCQ;
        }
        return choices.get(seededQuestionRandom(session.getId(), item).nextInt(choices.size()));
    }

    private LearnQuestionType preferWritten(Set<LearnQuestionType> enabled) {
        if (enabled.contains(LearnQuestionType.WRITTEN)) {
            return LearnQuestionType.WRITTEN;
        }
        if (enabled.contains(LearnQuestionType.TRUE_FALSE)) {
            return LearnQuestionType.TRUE_FALSE;
        }
        return LearnQuestionType.MCQ;
    }

    private LearnAnswerDirection selectAnswerDirection(LearnSession session, LearnSessionItem item) {
        if (session.getAnswerDirection() == LearnAnswerDirection.WORD_TO_MEANING) {
            return LearnAnswerDirection.WORD_TO_MEANING;
        }
        if (session.getAnswerDirection() == LearnAnswerDirection.MEANING_TO_WORD) {
            return LearnAnswerDirection.MEANING_TO_WORD;
        }
        // BOTH: English→Vietnamese while learning, Vietnamese→English as final confirmation
        LearnItemStage stage = normalizeStage(item.getStage());
        if (stage == LearnItemStage.FAMILIAR) {
            return LearnAnswerDirection.MEANING_TO_WORD;
        }
        return LearnAnswerDirection.WORD_TO_MEANING;
    }

    private TrueFalseQuestion buildTrueFalseQuestion(
            LearnSession session,
            LearnSessionItem item,
            LearnAnswerDirection direction,
            List<VocabItem> allDeckItems
    ) {
        Random seededRng = seededQuestionRandom(session.getId(), item);
        VocabItem vocab = item.getVocabItem();
        boolean isTrue = seededRng.nextBoolean();
        boolean wordToMeaning = direction == LearnAnswerDirection.WORD_TO_MEANING;

        if (isTrue) {
            String statement = wordToMeaning
                    ? "\"" + vocab.getWord() + "\" means \"" + vocab.getMeaningVi() + "\"."
                    : "\"" + vocab.getMeaningVi() + "\" is the meaning of \"" + vocab.getWord() + "\".";
            return new TrueFalseQuestion(statement, "True");
        }

        List<VocabItem> distractors = allDeckItems.stream()
                .filter(v -> !v.getId().equals(vocab.getId()))
                .toList();
        VocabItem wrong = distractors.isEmpty() ? vocab : distractors.get(seededRng.nextInt(distractors.size()));
        String statement = wordToMeaning
                ? "\"" + vocab.getWord() + "\" means \"" + wrong.getMeaningVi() + "\"."
                : "\"" + wrong.getMeaningVi() + "\" is the meaning of \"" + vocab.getWord() + "\".";
        return new TrueFalseQuestion(statement, "False");
    }

    private LearnItemStage nextCorrectStage(LearnSession session, LearnSessionItem item, LearnQuestionType answeredType) {
        LearnItemStage stage = normalizeStage(item.getStage());
        // 3-step progression: MCQ → Written EN→VI → Written VI→EN → Pass
        return switch (stage) {
            case NEW, SEEN -> LearnItemStage.LEARNING;      // Step 1 done → Step 2
            case LEARNING -> LearnItemStage.FAMILIAR;       // Step 2 done → Step 3
            case FAMILIAR -> LearnItemStage.MASTERED;       // Step 3 done → Pass!
            case MASTERED -> LearnItemStage.MASTERED;
            case NOT_STUDIED, STILL_LEARNING -> LearnItemStage.LEARNING;
        };
    }

    private LearnItemStage nextIncorrectStage(LearnItemStage currentStage) {
        // Wrong at any step → back to Step 1
        return LearnItemStage.NEW;
    }

    private LearnItemStage inferStageBeforeOverride(LearnItemStage currentStage) {
        return switch (normalizeStage(currentStage)) {
            case LEARNING -> LearnItemStage.FAMILIAR;
            case SEEN -> LearnItemStage.LEARNING;
            case NEW -> LearnItemStage.SEEN;
            case FAMILIAR, MASTERED -> normalizeStage(currentStage);
            case NOT_STUDIED, STILL_LEARNING -> LearnItemStage.LEARNING;
        };
    }

    private void captureReviewSnapshot(LearnAnswer answer, User user, VocabItem vocabItem) {
        progressRepo.findByUserIdAndVocabItemId(user.getId(), vocabItem.getId())
                .ifPresentOrElse(progress -> {
                    answer.setReviewSnapshotExists(true);
                    answer.setReviewSnapshotStatus(progress.getStatus().name());
                    answer.setReviewSnapshotKnownCount(progress.getKnownCount());
                    answer.setReviewSnapshotUnknownCount(progress.getUnknownCount());
                    answer.setReviewSnapshotDifficultCount(progress.getDifficultCount());
                    answer.setReviewSnapshotCorrectCount(progress.getCorrectCount());
                    answer.setReviewSnapshotWrongCount(progress.getWrongCount());
                    answer.setReviewSnapshotStreakCorrectCount(progress.getStreakCorrectCount());
                    answer.setReviewSnapshotEaseFactor(progress.getEaseFactor());
                    answer.setReviewSnapshotIntervalDays(progress.getIntervalDays());
                    answer.setReviewSnapshotRepetitionCount(progress.getRepetitionCount());
                    answer.setReviewSnapshotLapseCount(progress.getLapseCount());
                    answer.setReviewSnapshotLastQuality(progress.getLastQuality());
                    answer.setReviewSnapshotLastResponseTimeMs(progress.getLastResponseTimeMs());
                    answer.setReviewSnapshotLastMarkedAt(progress.getLastMarkedAt());
                    answer.setReviewSnapshotLastReviewedAt(progress.getLastReviewedAt());
                    answer.setReviewSnapshotNextReviewAt(progress.getNextReviewAt());
                }, () -> answer.setReviewSnapshotExists(false));
    }

    private void replayReviewAfterOverride(User user, LearnAnswer answer) {
        VocabItem vocabItem = answer.getSessionItem().getVocabItem();
        if (answer.getReviewSnapshotExists() == null) {
            reviewService.applyQuizResult(user, vocabItem, true, responseTimeMsAsInteger(answer));
            return;
        }

        progressRepo.findByUserIdAndVocabItemId(user.getId(), vocabItem.getId())
                .ifPresent(progress -> {
                    if (Boolean.TRUE.equals(answer.getReviewSnapshotExists())) {
                        restoreReviewSnapshot(answer, progress);
                        progressRepo.save(progress);
                    } else {
                        progressRepo.delete(progress);
                    }
                });
        progressRepo.flush();

        reviewService.applyQuizResult(user, vocabItem, true, responseTimeMsAsInteger(answer));
    }

    private void restoreReviewSnapshot(LearnAnswer answer, UserProgress progress) {
        progress.setStatus(VocabProgressStatus.valueOf(answer.getReviewSnapshotStatus()));
        progress.setKnownCount(valueOrZero(answer.getReviewSnapshotKnownCount()));
        progress.setUnknownCount(valueOrZero(answer.getReviewSnapshotUnknownCount()));
        progress.setDifficultCount(valueOrZero(answer.getReviewSnapshotDifficultCount()));
        progress.setCorrectCount(valueOrZero(answer.getReviewSnapshotCorrectCount()));
        progress.setWrongCount(valueOrZero(answer.getReviewSnapshotWrongCount()));
        progress.setStreakCorrectCount(valueOrZero(answer.getReviewSnapshotStreakCorrectCount()));
        progress.setEaseFactor(answer.getReviewSnapshotEaseFactor() == null ? 2.5 : answer.getReviewSnapshotEaseFactor());
        progress.setIntervalDays(valueOrZero(answer.getReviewSnapshotIntervalDays()));
        progress.setRepetitionCount(valueOrZero(answer.getReviewSnapshotRepetitionCount()));
        progress.setLapseCount(valueOrZero(answer.getReviewSnapshotLapseCount()));
        progress.setLastQuality(answer.getReviewSnapshotLastQuality());
        progress.setLastResponseTimeMs(answer.getReviewSnapshotLastResponseTimeMs());
        progress.setLastMarkedAt(answer.getReviewSnapshotLastMarkedAt());
        progress.setLastReviewedAt(answer.getReviewSnapshotLastReviewedAt());
        progress.setNextReviewAt(answer.getReviewSnapshotNextReviewAt());
    }

    private int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }

    private Integer responseTimeMsAsInteger(LearnAnswer answer) {
        return answer.getResponseTimeMs() == null ? null : answer.getResponseTimeMs().intValue();
    }

    private boolean isFinishedItem(LearnSession session, LearnSessionItem item) {
        LearnItemStage stage = normalizeStage(item.getStage());
        return stage == LearnItemStage.MASTERED
                || (session.getGoal() == LearnGoal.QUICK_REVIEW && stage == LearnItemStage.FAMILIAR);
    }

    private LearnQuestionResponse.Progress progressFor(LearnSession session, List<LearnSessionItem> items) {
        int mastered = 0;
        int newTerms = 0;
        int seen = 0;
        int learning = 0;
        int familiar = 0;

        for (LearnSessionItem item : items) {
            LearnItemStage stage = normalizeStage(item.getStage());
            if (isFinishedItem(session, item)) {
                mastered++;
            }
            switch (stage) {
                case NEW -> newTerms++;
                case SEEN -> seen++;
                case LEARNING -> learning++;
                case FAMILIAR -> familiar++;
                case MASTERED -> {
                }
                case NOT_STUDIED, STILL_LEARNING -> {
                }
            }
        }

        return new LearnQuestionResponse.Progress(
                mastered,
                session.getTotalTerms(),
                Math.max(0, session.getTotalTerms() - mastered),
                newTerms,
                seen,
                learning,
                familiar
        );
    }

    private void completeSession(LearnSession session, LearnQuestionResponse.Progress progress) {
        session.setMasteredTerms(progress.masteredTerms());
        session.setStatus(LearnSessionStatus.COMPLETED);
        session.setCompletedAt(LocalDateTime.now());
        session.setDurationMs(Duration.between(session.getStartedAt(), session.getCompletedAt()).toMillis());
        sessionRepo.save(session);
    }

    private void normalizeItemStages(List<LearnSessionItem> items) {
        boolean changed = false;
        for (LearnSessionItem item : items) {
            LearnItemStage normalized = normalizeStage(item.getStage());
            if (item.getStage() != normalized) {
                item.setStage(normalized);
                changed = true;
            }
        }
        if (changed) {
            sessionItemRepo.saveAll(items);
        }
    }

    private LearnItemStage normalizeStage(LearnItemStage stage) {
        if (stage == null || stage == LearnItemStage.NOT_STUDIED) {
            return LearnItemStage.NEW;
        }
        if (stage == LearnItemStage.STILL_LEARNING) {
            return LearnItemStage.LEARNING;
        }
        return stage;
    }

    private Set<LearnQuestionType> enabledQuestionTypes(LearnSession session) {
        if (!hasText(session.getEnabledQuestionTypes())) {
            return EnumSet.of(LearnQuestionType.MCQ, LearnQuestionType.WRITTEN);
        }

        EnumSet<LearnQuestionType> types = EnumSet.noneOf(LearnQuestionType.class);
        for (String value : session.getEnabledQuestionTypes().split(",")) {
            try {
                types.add(LearnQuestionType.valueOf(value.trim()));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return types.isEmpty()
                ? EnumSet.of(LearnQuestionType.MCQ, LearnQuestionType.WRITTEN)
                : types;
    }

    private String serializeQuestionTypes(List<LearnQuestionType> requestedTypes) {
        if (requestedTypes == null || requestedTypes.isEmpty()) {
            return DEFAULT_QUESTION_TYPES;
        }
        List<LearnQuestionType> distinctTypes = requestedTypes.stream()
                .filter(type -> type != null)
                .distinct()
                .toList();
        if (distinctTypes.isEmpty()) {
            return DEFAULT_QUESTION_TYPES;
        }
        return distinctTypes.stream()
                .map(Enum::name)
                .collect(Collectors.joining(","));
    }

    private LearnAnswerResponse answerResponse(LearnAnswer answer, LearnSessionItem item, LearnQuestionResponse.Progress progress) {
        return new LearnAnswerResponse(
                answer.isCorrect(),
                answer.getVerdict(),
                answer.getSimilarityScore(),
                answer.getUserAnswer() == null ? "" : answer.getUserAnswer(),
                answer.getCorrectAnswer(),
                normalizeStage(item.getStage()),
                item.getCorrectStreak(),
                buildVocabContext(item.getVocabItem()),
                progress
        );
    }

    @Transactional
    public void adjustQuality(Authentication auth, Long sessionId, AdjustLearnQualityRequest request) {
        User user = userService.currentUser(auth);
        LearnSession session = sessionRepo.findByIdAndUserId(sessionId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Learn session not found"));
        LearnSessionItem item = sessionItemRepo.findById(request.sessionItemId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session item not found"));
        if (!item.getSession().getId().equals(session.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Item does not belong to this session");
        }
        reviewService.applyQuizResult(user, item.getVocabItem(), true, null, request.quality());
    }

    private LearnQuestionResponse.VocabContext buildVocabContext(VocabItem vocab) {
        if (vocab == null) {
            return null;
        }
        return new LearnQuestionResponse.VocabContext(
                vocab.getIpa(),
                vocab.getMeaningVi(),
                vocab.getPartOfSpeech(),
                vocab.getExampleEn(),
                vocab.getExampleVi()
        );
    }

    private GradeResult gradeAnswer(String userAnswer, String correctAnswer, LearnQuestionType questionType, LearnGradingMode gradingMode) {
        if (questionType == LearnQuestionType.TRUE_FALSE || questionType == LearnQuestionType.MCQ) {
            boolean correct = normalizeExact(userAnswer).equals(normalizeExact(correctAnswer));
            return new GradeResult(
                    correct ? GradeVerdict.CORRECT : GradeVerdict.INCORRECT,
                    correct ? 1.0 : 0.0,
                    normalizeExact(userAnswer),
                    normalizeExact(correctAnswer)
            );
        }

        String userExact = normalizeExact(userAnswer);
        String correctExact = normalizeExact(correctAnswer);
        String userNormalized = normalizeForAnswer(userAnswer);
        String correctNormalized = normalizeForAnswer(correctAnswer);

        if (gradingMode == LearnGradingMode.EXACT) {
            if (userExact.equals(correctExact)) {
                return new GradeResult(GradeVerdict.CORRECT, 1.0, userExact, correctExact);
            }
            return closeOrIncorrect(userExact, correctExact, 2);
        }

        if (gradingMode == LearnGradingMode.FUZZY) {
            int maxDistance = correctNormalized.length() <= 5 ? 1 : 2;
            int distance = levenshteinDistance(userNormalized, correctNormalized);
            double similarity = similarity(distance, userNormalized, correctNormalized);
            if (distance <= maxDistance) {
                return new GradeResult(GradeVerdict.CORRECT, similarity, userNormalized, correctNormalized);
            }
            if (distance <= maxDistance + 2) {
                return new GradeResult(GradeVerdict.CLOSE, similarity, userNormalized, correctNormalized);
            }
            return new GradeResult(GradeVerdict.INCORRECT, similarity, userNormalized, correctNormalized);
        }

        if (userExact.equals(correctExact)) {
            return new GradeResult(GradeVerdict.CORRECT, 1.0, userNormalized, correctNormalized);
        }
        if (userNormalized.equals(correctNormalized)) {
            return new GradeResult(GradeVerdict.CLOSE, 1.0, userNormalized, correctNormalized);
        }
        return closeOrIncorrect(userNormalized, correctNormalized, 2);
    }

    private GradeResult closeOrIncorrect(String userAnswer, String correctAnswer, int closeDistance) {
        int distance = levenshteinDistance(userAnswer, correctAnswer);
        double similarity = similarity(distance, userAnswer, correctAnswer);
        return new GradeResult(
                distance <= closeDistance ? GradeVerdict.CLOSE : GradeVerdict.INCORRECT,
                similarity,
                userAnswer,
                correctAnswer
        );
    }

    private double similarity(int distance, String userAnswer, String correctAnswer) {
        int maxLength = Math.max(userAnswer == null ? 0 : userAnswer.length(), correctAnswer == null ? 0 : correctAnswer.length());
        if (maxLength == 0) {
            return 1.0;
        }
        return Math.max(0.0, 1.0 - ((double) distance / maxLength));
    }

    private String normalizeExact(String value) {
        return (value == null ? "" : value).trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private String normalizeForAnswer(String value) {
        String normalized = Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return normalized.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private int levenshteinDistance(String left, String right) {
        int[] previous = new int[right.length() + 1];
        int[] current = new int[right.length() + 1];

        for (int j = 0; j <= right.length(); j++) {
            previous[j] = j;
        }

        for (int i = 1; i <= left.length(); i++) {
            current[0] = i;
            for (int j = 1; j <= right.length(); j++) {
                int cost = left.charAt(i - 1) == right.charAt(j - 1) ? 0 : 1;
                current[j] = Math.min(
                        Math.min(current[j - 1] + 1, previous[j] + 1),
                        previous[j - 1] + cost
                );
            }
            int[] temp = previous;
            previous = current;
            current = temp;
        }

        return previous[right.length()];
    }

    private String questionToken(LearnSession session, LearnSessionItem item, GeneratedLearnQuestion question) {
        LearnItemStage stage = normalizeStage(item.getStage());
        String raw = String.join("|",
                String.valueOf(session.getId()),
                String.valueOf(item.getId()),
                String.valueOf(item.getTotalAttempts()),
                stage.name(),
                question.type().name(),
                normalizeExact(question.prompt()),
                normalizeExact(question.trueFalseStatement()),
                normalizeExact(question.correctAnswer())
        );
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    private String answerHint(GeneratedLearnQuestion question) {
        if (question.type() != LearnQuestionType.WRITTEN) {
            return null;
        }
        String answer = question.correctAnswer() == null ? "" : question.correctAnswer().trim();
        answer = answer.replaceFirst("^\\([^)]*\\)\\s*", "");
        int offset = 0;
        while (offset < answer.length()) {
            int codePoint = answer.codePointAt(offset);
            if (Character.isLetterOrDigit(codePoint)) {
                return new String(Character.toChars(codePoint));
            }
            offset += Character.charCount(codePoint);
        }
        return null;
    }

    private Random seededQuestionRandom(Long sessionId, LearnSessionItem item) {
        long seed = sessionId * 31 + item.getId() * 17 + item.getTotalAttempts();
        return new Random(seed);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private record GeneratedLearnQuestion(
            LearnQuestionType type,
            String prompt,
            List<String> options,
            String trueFalseStatement,
            String correctAnswer
    ) {
    }

    private record TrueFalseQuestion(String statement, String correctAnswer) {
    }

    private record GradeResult(GradeVerdict verdict, double similarity, String normalizedUser, String normalizedCorrect) {
    }
}
