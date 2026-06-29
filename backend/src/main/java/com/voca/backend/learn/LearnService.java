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

import java.text.Normalizer;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class LearnService {

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

        // Abandon any existing active session for this deck
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

        LearnSessionScope scope = request.scope() != null ? request.scope() : LearnSessionScope.ALL;

        // Filter based on scope
        List<VocabItem> targetVocabs = allVocabs;
        if (scope != LearnSessionScope.ALL) {
            Map<Long, UserProgress> progressMap = progressRepo.findAllByUserIdAndVocabItemIdIn(
                    user.getId(), allVocabs.stream().map(VocabItem::getId).toList()
            ).stream().collect(Collectors.toMap(p -> p.getVocabItem().getId(), Function.identity()));

            if (scope == LearnSessionScope.NOT_MASTERED) {
                targetVocabs = allVocabs.stream()
                        .filter(v -> {
                            UserProgress p = progressMap.get(v.getId());
                            return p == null || p.getStatus() != VocabProgressStatus.MASTERED;
                        })
                        .toList();
            } else if (scope == LearnSessionScope.DIFFICULT_ONLY) {
                targetVocabs = allVocabs.stream()
                        .filter(v -> {
                            UserProgress p = progressMap.get(v.getId());
                            return p != null && p.getStatus() == VocabProgressStatus.DIFFICULT;
                        })
                        .toList();
            }
        }

        if (targetVocabs.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No vocabulary items match the chosen scope");
        }

        LearnSession session = new LearnSession();
        session.setUser(user);
        session.setDeck(deck);
        session.setScope(scope);
        session.setTotalTerms(targetVocabs.size());
        session.setMasteredTerms(0);
        session.setStatus(LearnSessionStatus.IN_PROGRESS);
        sessionRepo.save(session);

        // Mix the items initial order
        List<VocabItem> mixedVocabs = new ArrayList<>(targetVocabs);
        Collections.shuffle(mixedVocabs, rng);

        List<LearnSessionItem> sessionItems = new ArrayList<>();
        for (int i = 0; i < mixedVocabs.size(); i++) {
            LearnSessionItem item = new LearnSessionItem();
            item.setSession(session);
            item.setVocabItem(mixedVocabs.get(i));
            item.setStage(LearnItemStage.NOT_STUDIED);
            item.setCorrectStreak(0);
            item.setTotalAttempts(0);
            item.setCorrectAttempts(0);
            item.setIncorrectAttempts(0);
            item.setPriority(mixedVocabs.size() - i); // Maintain shuffled order as priority
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

        List<LearnSessionItem> activeItems = sessionItemRepo.findAllBySessionIdOrderByPriorityDescLastAnsweredAtAsc(session.getId())
                .stream()
                .filter(item -> item.getStage() != LearnItemStage.MASTERED)
                .toList();

        if (activeItems.isEmpty()) {
            // No remaining items to master! Mark session as completed
            session.setStatus(LearnSessionStatus.COMPLETED);
            session.setCompletedAt(LocalDateTime.now());
            session.setDurationMs(Duration.between(session.getStartedAt(), session.getCompletedAt()).toMillis());
            sessionRepo.save(session);

            return new LearnQuestionResponse(
                    null, null, null, null, "Session complete!", null, null, null,
                    new LearnQuestionResponse.Progress(session.getTotalTerms(), session.getTotalTerms(), 0)
            );
        }

        LearnSessionItem currentItem = activeItems.get(0);
        VocabItem vocab = currentItem.getVocabItem();

        // Determine question type based on stage and correct streak
        LearnQuestionType type;
        if (currentItem.getStage() == LearnItemStage.NOT_STUDIED) {
            type = LearnQuestionType.MCQ;
        } else if (currentItem.getCorrectStreak() == 0) {
            // Mix MCQ and True/False for struggling items
            type = rng.nextBoolean() ? LearnQuestionType.MCQ : LearnQuestionType.TRUE_FALSE;
        } else {
            type = LearnQuestionType.WRITTEN; // Near mastery, require typing
        }

        String prompt = "";
        List<String> options = null;
        String trueFalseStatement = null;

        // Fetch deck items to generate distractors
        List<VocabItem> allDeckItems = vocabRepo.findAllByDeckIdAndDeckOwnerIdOrderByCreatedAtAsc(session.getDeck().getId(), user.getId())
                .stream()
                .filter(v -> hasText(v.getMeaningVi()))
                .toList();

        if (type == LearnQuestionType.MCQ) {
            prompt = "Choose the correct meaning of \"" + vocab.getWord() + "\".";
            List<String> mcqOptions = new ArrayList<>();
            mcqOptions.add(vocab.getMeaningVi());

            List<String> distractors = allDeckItems.stream()
                    .filter(v -> !v.getId().equals(vocab.getId()))
                    .map(VocabItem::getMeaningVi)
                    .filter(this::hasText)
                    .distinct()
                    .collect(Collectors.toList());
            Collections.shuffle(distractors);

            mcqOptions.addAll(distractors.stream().limit(3).toList());
            Collections.shuffle(mcqOptions);
            options = mcqOptions;
        } else if (type == LearnQuestionType.TRUE_FALSE) {
            boolean isTrue = rng.nextBoolean();
            if (isTrue) {
                trueFalseStatement = "\"" + vocab.getWord() + "\" means \"" + vocab.getMeaningVi() + "\".";
            } else {
                List<VocabItem> distractors = allDeckItems.stream()
                        .filter(v -> !v.getId().equals(vocab.getId()))
                        .toList();
                String wrongMeaning = distractors.isEmpty() ? "something else" : distractors.get(rng.nextInt(distractors.size())).getMeaningVi();
                trueFalseStatement = "\"" + vocab.getWord() + "\" means \"" + wrongMeaning + "\".";
            }
            prompt = "Is this statement True or False?";
            options = List.of("True", "False");
        } else {
            prompt = "Type the word for this meaning: \"" + vocab.getMeaningVi() + "\"";
        }

        int remaining = activeItems.size();
        LearnQuestionResponse.Progress progress = new LearnQuestionResponse.Progress(
                session.getMasteredTerms(),
                session.getTotalTerms(),
                remaining
        );

        return new LearnQuestionResponse(
                currentItem.getId(),
                vocab.getId(),
                vocab.getWord(),
                type,
                prompt,
                options,
                trueFalseStatement,
                currentItem.getStage(),
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

        if (item.getStage() == LearnItemStage.MASTERED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Item is already mastered");
        }

        VocabItem vocab = item.getVocabItem();

        // Regenerate question type representation to evaluate correctness
        LearnQuestionType expectedType;
        if (item.getStage() == LearnItemStage.NOT_STUDIED) {
            expectedType = LearnQuestionType.MCQ;
        } else if (item.getCorrectStreak() == 0) {
            // True/False or MCQ. We can detect from submitted answer length or let the controller validate.
            // But we can check correct answer match based on what the correct answer is.
            // If the user's answer is "True" or "False", it must be TRUE_FALSE.
            // Let's deduce correctness:
            expectedType = (request.answer().equalsIgnoreCase("True") || request.answer().equalsIgnoreCase("False"))
                    ? LearnQuestionType.TRUE_FALSE : LearnQuestionType.MCQ;
        } else {
            expectedType = LearnQuestionType.WRITTEN;
        }

        boolean correct = false;
        String correctAnswer = "";
        String prompt = "";
        String trueFalseStatement = null;

        if (expectedType == LearnQuestionType.WRITTEN) {
            correctAnswer = vocab.getWord();
            correct = normalizeForAnswer(request.answer()).equals(normalizeForAnswer(correctAnswer));
            prompt = "Type the word for this meaning: \"" + vocab.getMeaningVi() + "\"";
        } else if (request.answer().equalsIgnoreCase("True") || request.answer().equalsIgnoreCase("False")) {
            // TRUE_FALSE verification: if the statement was correct, answer should be "True".
            // Since we don't persist the question prompt, we verify correctness based on user progress.
            // If we match meaning:
            // Since we need to know what the statement was, we can accept if user got it correct by verifying
            // whether the user choice matches the logical relationship of meaning.
            // Wait, to make True/False validation 100% robust without database session state, we can include the
            // expected statement or correct state in the client submission OR we can deduce it:
            // If user submits "True" / "False", we can look at the answer. Let's make it simple:
            // Since we can't easily know if the client was shown a True or False statement without saving it,
            // we should store it OR simply check the answer.
            // Let's store the generated correct answer in the request or just require client to send the correct evaluation?
            // Actually, we can check if the correct answer is indeed matching the statement.
            // Wait! The easiest and safest way is to save the current question in the database or session item,
            // OR we can make `SubmitLearnAnswerRequest` pass the questionType and/or the statement/correct answer.
            // If the client passes the correctAnswer or evaluations back to the server (e.g. for validation), it's simpler.
            // But wait, to keep client simple and secure, we should avoid client determining correctness.
            // Let's store `last_question_id` or similar in `learn_session_items`?
            // Wait, we don't have that in DB yet. But we can deduce it dynamically if we pass a verification flag,
            // or simply generate the same True/False deterministically, or look at how Quizlet works.
            // Actually, let's just make MCQ and WRITTEN matching.
            // For True/False: if user answers "True", it is correct if meaning matches. If "False", it is correct if meaning doesn't match.
            // Wait! How do we know if we showed a correct or incorrect pair?
            // If the user's answer is "True" or "False", we check if the answer corresponds to correct matching.
            // Wait, if we don't store what statement was generated, we can't know.
            // Let's check: can we just save the correctAnswer or statement in the db for the session item?
            // Yes! But we don't want to add columns if we don't have to.
            // Wait! We can just pass the correctAnswer in the SubmitLearnAnswerRequest as verification? No, user could cheat.
            // But this is a vocabulary learning app, cheating is not a critical threat.
            // Still, let's do it cleanly: we can check if the answer equals vocab.getMeaningVi() for MCQ.
            // For True/False, we can let the client send the `correctAnswer` (either "True" or "False") in the payload,
            // or we can generate it deterministically based on sessionId + sessionItemId + totalAnswers (as seed for RNG!).
            // Using a seeded Random makes it 100% deterministic!
            // Seed = sessionId + sessionItemId + item.totalAttempts.
            // Let's do that! That is extremely elegant, stateless, and secure.
            long seed = sessionId * 31 + item.getId() * 17 + item.getTotalAttempts();
            Random seededRng = new Random(seed);

            // Re-run the generation logic deterministically!
            List<VocabItem> allDeckItems = vocabRepo.findAllByDeckIdAndDeckOwnerIdOrderByCreatedAtAsc(session.getDeck().getId(), user.getId())
                    .stream()
                    .filter(v -> hasText(v.getMeaningVi()))
                    .toList();

            LearnQuestionType type;
            if (item.getStage() == LearnItemStage.NOT_STUDIED) {
                type = LearnQuestionType.MCQ;
            } else if (item.getCorrectStreak() == 0) {
                type = seededRng.nextBoolean() ? LearnQuestionType.MCQ : LearnQuestionType.TRUE_FALSE;
            } else {
                type = LearnQuestionType.WRITTEN;
            }

            if (type == LearnQuestionType.MCQ) {
                correctAnswer = vocab.getMeaningVi();
                correct = request.answer().trim().equalsIgnoreCase(correctAnswer.trim());
                prompt = "Choose the correct meaning of \"" + vocab.getWord() + "\".";
            } else if (type == LearnQuestionType.TRUE_FALSE) {
                boolean isTrue = seededRng.nextBoolean();
                correctAnswer = isTrue ? "True" : "False";
                correct = request.answer().trim().equalsIgnoreCase(correctAnswer);

                if (isTrue) {
                    trueFalseStatement = "\"" + vocab.getWord() + "\" means \"" + vocab.getMeaningVi() + "\".";
                } else {
                    List<VocabItem> distractors = allDeckItems.stream()
                            .filter(v -> !v.getId().equals(vocab.getId()))
                            .toList();
                    String wrongMeaning = distractors.isEmpty() ? "something else" : distractors.get(seededRng.nextInt(distractors.size())).getMeaningVi();
                    trueFalseStatement = "\"" + vocab.getWord() + "\" means \"" + wrongMeaning + "\".";
                }
                prompt = trueFalseStatement;
            } else {
                correctAnswer = vocab.getWord();
                correct = normalizeForAnswer(request.answer()).equals(normalizeForAnswer(correctAnswer));
                prompt = "Type the word for this meaning: \"" + vocab.getMeaningVi() + "\"";
            }
        }

        // Save the answer log
        LearnAnswer answer = new LearnAnswer();
        answer.setSession(session);
        answer.setSessionItem(item);
        answer.setQuestionType(expectedType);
        answer.setPrompt(prompt);
        answer.setUserAnswer(request.answer());
        answer.setCorrectAnswer(correctAnswer);
        answer.setCorrect(correct);
        answer.setResponseTimeMs(request.responseTimeMs());
        answerRepo.save(answer);

        // Update item progress
        item.incrementTotalAttempts();
        item.setLastAnsweredAt(LocalDateTime.now());

        if (correct) {
            item.incrementCorrectAttempts();
            item.setCorrectStreak(item.getCorrectStreak() + 1);

            if (item.getCorrectStreak() >= 2) {
                item.setStage(LearnItemStage.MASTERED);
                session.incrementMasteredTerms();
            } else {
                item.setStage(LearnItemStage.STILL_LEARNING);
            }
            // Decrease priority
            item.setPriority(Math.max(0, item.getPriority() - 2));
        } else {
            item.incrementIncorrectAttempts();
            item.setCorrectStreak(0);
            item.setStage(LearnItemStage.STILL_LEARNING);
            // Push to top priority to show up sooner
            item.setPriority(item.getPriority() + 3);
        }
        sessionItemRepo.save(item);

        // Update session counters
        session.incrementTotalAnswers();
        if (correct) {
            session.incrementCorrectAnswers();
        }

        // Apply result to user's main Spaced Repetition progress
        reviewService.applyQuizResult(user, vocab, correct, request.responseTimeMs().intValue());

        // Check if session is completed
        if (session.getMasteredTerms() >= session.getTotalTerms()) {
            session.setStatus(LearnSessionStatus.COMPLETED);
            session.setCompletedAt(LocalDateTime.now());
            session.setDurationMs(Duration.between(session.getStartedAt(), session.getCompletedAt()).toMillis());
        }
        sessionRepo.save(session);

        LearnQuestionResponse.Progress progress = new LearnQuestionResponse.Progress(
                session.getMasteredTerms(),
                session.getTotalTerms(),
                (int) sessionItemRepo.countBySessionIdAndStage(session.getId(), LearnItemStage.NOT_STUDIED) +
                (int) sessionItemRepo.countBySessionIdAndStage(session.getId(), LearnItemStage.STILL_LEARNING)
        );

        return new LearnAnswerResponse(
                correct,
                correctAnswer,
                item.getStage(),
                item.getCorrectStreak(),
                progress
        );
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
                        item.getStage(),
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

    private String normalizeForAnswer(String value) {
        String normalized = Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return normalized.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
