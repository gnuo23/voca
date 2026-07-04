package com.voca.backend.quiz;

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
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class QuizService {

    private final DeckService deckService;
    private final UserService userService;
    private final VocabItemRepository vocabItemRepository;
    private final QuestionRepository questionRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final QuizAnswerRepository quizAnswerRepository;
    private final ReviewService reviewService;

    public QuizService(
            DeckService deckService,
            UserService userService,
            VocabItemRepository vocabItemRepository,
            QuestionRepository questionRepository,
            QuizAttemptRepository quizAttemptRepository,
            QuizAnswerRepository quizAnswerRepository,
            ReviewService reviewService
    ) {
        this.deckService = deckService;
        this.userService = userService;
        this.vocabItemRepository = vocabItemRepository;
        this.questionRepository = questionRepository;
        this.quizAttemptRepository = quizAttemptRepository;
        this.quizAnswerRepository = quizAnswerRepository;
        this.reviewService = reviewService;
    }

    @Transactional(readOnly = true)
    public QuizImportPreviewResponse previewImport(Authentication authentication, Long deckId, QuizImportRequest request) {
        User user = userService.currentUser(authentication);
        deckService.findOwnedDeck(user, deckId);
        List<VocabItem> deckItems = loadDeckItems(deckId);
        return buildImportPreview(deckItems, request);
    }

    @Transactional
    public QuizImportPreviewResponse saveImport(Authentication authentication, Long deckId, QuizImportRequest request) {
        User user = userService.currentUser(authentication);
        Deck deck = deckService.findOwnedDeck(user, deckId);
        List<VocabItem> deckItems = loadDeckItems(deckId);
        QuizImportPreviewResponse preview = buildImportPreview(deckItems, request);

        Map<Long, VocabItem> deckItemById = deckItems.stream().collect(Collectors.toMap(VocabItem::getId, item -> item));
        List<QuizImportItemResponse> okItems = preview.items().stream()
                .filter(item -> item.status() == QuizImportStatus.OK)
                .toList();
        if (okItems.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No matching vocabulary lines found for quiz import");
        }

        List<Long> vocabIds = okItems.stream().map(QuizImportItemResponse::vocabId).toList();
        overwriteExistingQuestions(deck, user, vocabIds);

        List<Question> newQuestions = new ArrayList<>();
        for (QuizImportItemResponse item : okItems) {
            VocabItem vocab = deckItemById.get(item.vocabId());
            if (vocab == null) {
                continue;
            }
            Question question = new Question();
            question.setDeck(deck);
            question.setOwner(user);
            question.setVocabItem(vocab);
            question.setType(QuestionType.CHOOSE_MEANING);
            question.setPrompt(item.prompt());
            question.setCorrectAnswer(vocab.getWord());
            question.setOptionsJson(null);
            question.setExplanation("Đáp án đúng: \"" + vocab.getWord() + "\".");
            newQuestions.add(question);
        }
        questionRepository.saveAll(newQuestions);
        return preview;
    }

    @Transactional
    public QuizAttemptResponse startQuiz(Authentication authentication, Long deckId, StartQuizRequest request) {
        User user = userService.currentUser(authentication);
        Deck deck = deckService.findStudyDeck(user, deckId);
        List<VocabItem> deckItems = loadDeckItems(deck.getId());
        if (deckItems.size() < 4) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Need at least 4 vocabulary items to start a quiz");
        }

        List<Question> questions = new ArrayList<>(
                questionRepository.findAllByDeckIdAndOwnerIdOrderByCreatedAtAsc(deck.getId(), deck.getOwner().getId())
        );
        if (questions.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No quiz questions saved for this deck yet");
        }
        Collections.shuffle(questions);

        if (request != null && request.limit() != null && request.limit() > 0 && request.limit() < questions.size()) {
            questions = new ArrayList<>(questions.subList(0, request.limit()));
        }

        QuizAttempt attempt = new QuizAttempt();
        attempt.setDeck(deck);
        attempt.setUser(user);
        attempt.setTotalQuestions(questions.size());
        QuizAttempt savedAttempt = quizAttemptRepository.save(attempt);

        return toAttemptResponse(savedAttempt, 0, questions, deckItems);
    }

    @Transactional
    public QuizAnswerResponse answer(Authentication authentication, Long attemptId, AnswerQuizQuestionRequest request) {
        User user = userService.currentUser(authentication);
        QuizAttempt attempt = findOwnedAttempt(user, attemptId);
        Question question = questionRepository.findById(request.questionId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found"));

        if (!question.getDeck().getId().equals(attempt.getDeck().getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Question does not belong to this attempt deck");
        }
        if (!question.getOwner().getId().equals(attempt.getDeck().getOwner().getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Question does not belong to this attempt deck owner");
        }
        if (quizAnswerRepository.findByAttemptIdAndQuestionId(attempt.getId(), question.getId()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Question already answered");
        }

        String submittedAnswer = request.answer() == null ? "" : request.answer().trim();
        boolean correct = submittedAnswer.equalsIgnoreCase(question.getCorrectAnswer().trim());

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

    private List<VocabItem> loadDeckItems(Long deckId) {
        return vocabItemRepository.findAllByDeckIdOrderByCreatedAtAsc(deckId);
    }

    private QuizImportPreviewResponse buildImportPreview(List<VocabItem> deckItems, QuizImportRequest request) {
        String rawText = request == null || request.rawText() == null ? "" : request.rawText();
        Map<String, VocabItem> byWord = vocabByNormalizedWord(deckItems);
        Set<Long> seenVocabIds = new LinkedHashSet<>();
        List<QuizImportItemResponse> items = new ArrayList<>();

        String[] lines = rawText.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
        for (int index = 0; index < lines.length; index++) {
            int lineNumber = index + 1;
            String line = lines[index].trim();
            if (line.isEmpty()) {
                continue;
            }

            ParsedQuizImportLine parsed = parseQuizImportLine(line);
            if (parsed.error() != null) {
                items.add(new QuizImportItemResponse(lineNumber, null, parsed.word(), parsed.prompt(), QuizImportStatus.ERROR, parsed.error()));
                continue;
            }

            VocabItem deckItem = byWord.get(normalizeWord(parsed.word()));
            if (deckItem == null) {
                items.add(new QuizImportItemResponse(lineNumber, null, parsed.word(), parsed.prompt(), QuizImportStatus.SKIPPED, "Word is not in this deck"));
                continue;
            }
            if (!seenVocabIds.add(deckItem.getId())) {
                items.add(new QuizImportItemResponse(lineNumber, deckItem.getId(), deckItem.getWord(), parsed.prompt(), QuizImportStatus.SKIPPED, "Duplicate word in quiz import"));
                continue;
            }

            items.add(new QuizImportItemResponse(lineNumber, deckItem.getId(), deckItem.getWord(), parsed.prompt(), QuizImportStatus.OK, null));
        }

        if (items.isEmpty()) {
            items.add(new QuizImportItemResponse(1, null, null, null, QuizImportStatus.ERROR, "No quiz lines found"));
        }

        int validCount = (int) items.stream().filter(item -> item.status() == QuizImportStatus.OK).count();
        int skippedCount = (int) items.stream().filter(item -> item.status() == QuizImportStatus.SKIPPED).count();
        int errorCount = (int) items.stream().filter(item -> item.status() == QuizImportStatus.ERROR).count();
        return new QuizImportPreviewResponse(items, validCount, skippedCount, errorCount);
    }

    private ParsedQuizImportLine parseQuizImportLine(String line) {
        String[] parts = line.split("\\s*--\\s*", 2);
        if (parts.length != 2) {
            return new ParsedQuizImportLine(null, null, "Use format: word -- question");
        }
        String word = cleanImportText(parts[0], 255);
        String prompt = cleanImportText(parts[1], 1000);
        if (!hasText(word)) {
            return new ParsedQuizImportLine(word, prompt, "Missing word");
        }
        if (!hasText(prompt)) {
            return new ParsedQuizImportLine(word, prompt, "Missing question");
        }
        return new ParsedQuizImportLine(word, prompt, null);
    }

    private void overwriteExistingQuestions(Deck deck, User user, List<Long> vocabIds) {
        Set<Long> uniqueIds = new LinkedHashSet<>(vocabIds);
        if (uniqueIds.isEmpty()) {
            return;
        }
        List<Question> existing = questionRepository.findAllByDeckIdAndOwnerIdAndVocabItemIdIn(deck.getId(), user.getId(), uniqueIds);
        if (existing.isEmpty()) {
            return;
        }
        List<Long> questionIds = existing.stream().map(Question::getId).toList();
        quizAnswerRepository.deleteByQuestionIdIn(questionIds);
        questionRepository.deleteAll(existing);
    }

    private String cleanImportText(String value, int maxLength) {
        if (!hasText(value)) {
            return null;
        }
        String cleaned = value.trim().replaceAll("\\s+", " ");
        return cleaned.length() > maxLength ? cleaned.substring(0, maxLength) : cleaned;
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

    private QuizAttemptResponse toAttemptResponse(QuizAttempt attempt, int answeredCount, List<Question> questions, List<VocabItem> deckItems) {
        return new QuizAttemptResponse(
                attempt.getId(),
                attempt.getDeck().getId(),
                attempt.getTotalQuestions(),
                answeredCount,
                attempt.getCorrectCount(),
                attempt.getCompletedAt() != null,
                attempt.getCompletedAt(),
                attempt.getCreatedAt(),
                questions.stream().map(question -> toQuestionResponse(question, deckItems)).toList()
        );
    }

    private QuizQuestionResponse toQuestionResponse(Question question, List<VocabItem> deckItems) {
        return new QuizQuestionResponse(
                question.getId(),
                question.getDeck().getId(),
                question.getVocabItem().getId(),
                question.getType(),
                question.getPrompt(),
                buildOptionsForQuestion(question, deckItems),
                question.getExplanation(),
                question.getCreatedAt()
        );
    }

    private List<String> buildOptionsForQuestion(Question question, List<VocabItem> deckItems) {
        String correct = question.getCorrectAnswer();
        List<String> distractors = deckItems.stream()
                .filter(item -> !item.getId().equals(question.getVocabItem().getId()))
                .map(VocabItem::getWord)
                .filter(this::hasText)
                .filter(word -> !word.equalsIgnoreCase(correct))
                .distinct()
                .collect(Collectors.toCollection(ArrayList::new));
        Collections.shuffle(distractors);

        List<String> options = new ArrayList<>();
        options.add(correct);
        options.addAll(distractors.stream().limit(3).toList());
        Collections.shuffle(options);
        return options;
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

    private Map<String, VocabItem> vocabByNormalizedWord(List<VocabItem> items) {
        Map<String, VocabItem> byWord = new HashMap<>();
        for (VocabItem item : items) {
            byWord.putIfAbsent(normalizeWord(item.getWord()), item);
        }
        return byWord;
    }

    private String normalizeWord(String word) {
        if (word == null) {
            return "";
        }
        String normalized = Normalizer.normalize(word, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return normalized.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private record ParsedQuizImportLine(String word, String prompt, String error) {
    }
}
