package com.voca.backend.deck;

import com.voca.backend.classroom.ClassroomDeckRepository;
import com.voca.backend.learn.LearnSessionRepository;
import com.voca.backend.quiz.QuestionRepository;
import com.voca.backend.user.User;
import com.voca.backend.user.UserService;
import com.voca.backend.vocab.UserProgressRepository;
import com.voca.backend.vocab.VocabItemRepository;
import com.voca.backend.vocab.VocabProgressStatus;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

@Service
public class DeckService {

    private final DeckRepository deckRepository;
    private final UserService userService;
    private final VocabItemRepository vocabItemRepository;
    private final UserProgressRepository userProgressRepository;
    private final QuestionRepository questionRepository;
    private final LearnSessionRepository learnSessionRepository;
    private final ClassroomDeckRepository classroomDeckRepository;

    public DeckService(
            DeckRepository deckRepository,
            UserService userService,
            VocabItemRepository vocabItemRepository,
            UserProgressRepository userProgressRepository,
            QuestionRepository questionRepository,
            LearnSessionRepository learnSessionRepository,
            ClassroomDeckRepository classroomDeckRepository
    ) {
        this.deckRepository = deckRepository;
        this.userService = userService;
        this.vocabItemRepository = vocabItemRepository;
        this.userProgressRepository = userProgressRepository;
        this.questionRepository = questionRepository;
        this.learnSessionRepository = learnSessionRepository;
        this.classroomDeckRepository = classroomDeckRepository;
    }

    @Transactional
    public DeckResponse create(Authentication authentication, DeckRequest request) {
        User owner = userService.currentUser(authentication);

        Deck deck = new Deck();
        deck.setOwner(owner);
        apply(deck, request);

        return toResponse(deckRepository.save(deck), owner);
    }

    @Transactional(readOnly = true)
    public List<DeckResponse> list(Authentication authentication) {
        User owner = userService.currentUser(authentication);
        return deckRepository.findAllByOwnerIdOrderByUpdatedAtDesc(owner.getId())
                .stream()
                .map(deck -> toResponse(deck, owner))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DeckResponse> listStudyDecks(Authentication authentication) {
        User user = userService.currentUser(authentication);
        List<Deck> ownedDecks = deckRepository.findAllByOwnerIdOrderByUpdatedAtDesc(user.getId());
        List<Long> classDeckIds = classroomDeckRepository.findStudyDeckIds(user.getId());
        List<Deck> classDecks = classDeckIds.isEmpty() ? List.of() : deckRepository.findAllById(classDeckIds);
        return java.util.stream.Stream.concat(ownedDecks.stream(), classDecks.stream())
                .collect(java.util.stream.Collectors.toMap(Deck::getId, Function.identity(), (left, right) -> left))
                .values()
                .stream()
                .sorted((left, right) -> right.getUpdatedAt().compareTo(left.getUpdatedAt()))
                .map(deck -> toResponse(deck, user))
                .toList();
    }

    @Transactional(readOnly = true)
    public DeckResponse get(Authentication authentication, Long deckId) {
        User user = userService.currentUser(authentication);
        Deck deck = findStudyDeck(user, deckId);
        return toResponse(deck, user);
    }

    @Transactional
    public DeckResponse update(Authentication authentication, Long deckId, DeckRequest request) {
        Deck deck = findOwnedDeck(authentication, deckId);
        apply(deck, request);
        return toResponse(deck, deck.getOwner());
    }

    @Transactional
    public void delete(Authentication authentication, Long deckId) {
        Deck deck = findOwnedDeck(authentication, deckId);
        deckRepository.delete(deck);
    }

    @Transactional(readOnly = true)
    public Deck findOwnedDeck(Authentication authentication, Long deckId) {
        User owner = userService.currentUser(authentication);
        return findOwnedDeck(owner, deckId);
    }

    @Transactional(readOnly = true)
    public Deck findOwnedDeck(User owner, Long deckId) {
        return deckRepository.findByIdAndOwnerId(deckId, owner.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Deck not found"));
    }

    @Transactional(readOnly = true)
    public Deck findStudyDeck(Authentication authentication, Long deckId) {
        User user = userService.currentUser(authentication);
        return findStudyDeck(user, deckId);
    }

    @Transactional(readOnly = true)
    public Deck findStudyDeck(User user, Long deckId) {
        return deckRepository.findById(deckId)
                .filter(deck -> canStudyDeck(user, deck))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Deck not found"));
    }

    @Transactional(readOnly = true)
    public boolean canStudyDeck(User user, Deck deck) {
        return deck.getOwner().getId().equals(user.getId())
                || classroomDeckRepository.existsStudyAccess(deck.getId(), user.getId());
    }

    @Transactional
    public DeckResponse resetDeckProgress(Authentication authentication, Long deckId) {
        User owner = userService.currentUser(authentication);
        Deck deck = findStudyDeck(owner, deckId);
        List<Long> vocabIds = vocabItemRepository.findAllByDeckIdOrderByCreatedAtAsc(deck.getId())
                .stream()
                .map(item -> item.getId())
                .toList();
        if (!vocabIds.isEmpty()) {
            userProgressRepository.deleteAllByUserIdAndVocabItemIdIn(owner.getId(), vocabIds);
        }
        learnSessionRepository.deleteAllByUserIdAndDeckId(owner.getId(), deck.getId());
        return toResponse(deck, owner);
    }

    private void apply(Deck deck, DeckRequest request) {
        deck.setName(request.name().trim());
        deck.setDescription(request.description() == null ? null : request.description().trim());
    }

    public DeckResponse toResponse(Deck deck, User owner) {
        long totalWords = vocabItemRepository.countByDeckId(deck.getId());
        List<Long> vocabIds = vocabItemRepository.findAllByDeckIdOrderByCreatedAtAsc(deck.getId())
                .stream()
                .map(item -> item.getId())
                .toList();
        long learnedWords = vocabIds.isEmpty() ? 0 : userProgressRepository.findAllByUserIdAndVocabItemIdIn(owner.getId(), vocabIds)
                .stream()
                .filter(progress -> Set.of(VocabProgressStatus.REVIEW, VocabProgressStatus.MASTERED).contains(progress.getStatus()))
                .count();
        long dueWords = totalWords - learnedWords;
        long dueTodayCount = vocabIds.isEmpty() ? 0 : userProgressRepository.findAllByUserIdAndVocabItemIdIn(owner.getId(), vocabIds)
                .stream()
                .filter(progress -> progress.getStatus() != VocabProgressStatus.NEW)
                .filter(progress -> progress.getNextReviewAt() != null)
                .filter(progress -> !progress.getNextReviewAt().isAfter(java.time.LocalDateTime.now()))
                .count();
        long savedQuestionCount = questionRepository.countByDeckIdAndOwnerId(deck.getId(), deck.getOwner().getId());
        return DeckResponse.from(deck, totalWords, learnedWords, dueWords, dueTodayCount, savedQuestionCount, owner.getId());
    }
}
