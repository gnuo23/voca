package com.voca.backend.deck;

import com.voca.backend.classroom.ClassroomDeckRepository;
import com.voca.backend.learn.LearnProgressRepository;
import com.voca.backend.learn.LearnSessionRepository;
import com.voca.backend.quiz.QuestionRepository;
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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

@Service
public class DeckService {

    private static final DateTimeFormatter DIFFICULT_DECK_DAY_FORMAT = DateTimeFormatter.ofPattern("dd");

    private final DeckRepository deckRepository;
    private final UserService userService;
    private final VocabItemRepository vocabItemRepository;
    private final UserProgressRepository userProgressRepository;
    private final LearnProgressRepository learnProgressRepository;
    private final QuestionRepository questionRepository;
    private final LearnSessionRepository learnSessionRepository;
    private final ClassroomDeckRepository classroomDeckRepository;

    public DeckService(
            DeckRepository deckRepository,
            UserService userService,
            VocabItemRepository vocabItemRepository,
            UserProgressRepository userProgressRepository,
            LearnProgressRepository learnProgressRepository,
            QuestionRepository questionRepository,
            LearnSessionRepository learnSessionRepository,
            ClassroomDeckRepository classroomDeckRepository
    ) {
        this.deckRepository = deckRepository;
        this.userService = userService;
        this.vocabItemRepository = vocabItemRepository;
        this.userProgressRepository = userProgressRepository;
        this.learnProgressRepository = learnProgressRepository;
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

    @Transactional
    public DeckResponse createDifficultDeck(Authentication authentication, Long sourceDeckId) {
        User owner = userService.currentUser(authentication);
        Deck sourceDeck = sourceDeckId == null ? null : findStudyDeck(owner, sourceDeckId);
        List<UserProgress> difficultProgress = userProgressRepository
                .findAllByUserIdAndStatusOrderByLastMarkedAtDesc(owner.getId(), VocabProgressStatus.DIFFICULT)
                .stream()
                .filter(progress -> canStudyDeck(owner, progress.getVocabItem().getDeck()))
                .filter(progress -> !isGeneratedDifficultDeck(progress.getVocabItem().getDeck()))
                .filter(progress -> sourceDeck == null || progress.getVocabItem().getDeck().getId().equals(sourceDeck.getId()))
                .collect(java.util.stream.Collectors.toMap(
                        progress -> progress.getVocabItem().getNormalizedWord(),
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new
                ))
                .values()
                .stream()
                .toList();

        if (difficultProgress.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No difficult words found");
        }

        Deck deck = new Deck();
        deck.setOwner(owner);
        deck.setName(generateDifficultDeckName(owner.getId()));
        deck.setDescription(sourceDeck == null
                ? "Auto-generated from difficult words"
                : "Auto-generated from difficult words in " + sourceDeck.getName());
        Deck savedDeck = deckRepository.save(deck);

        difficultProgress.forEach(sourceProgress -> vocabItemRepository.save(copyDifficultItem(sourceProgress.getVocabItem(), savedDeck)));

        return toResponse(savedDeck, owner);
    }

    @Transactional(readOnly = true)
    public List<DeckResponse> list(Authentication authentication) {
        User owner = userService.currentUser(authentication);
        return deckRepository.findAllByOwnerIdOrderByUpdatedAtDesc(owner.getId())
                .stream()
                .filter(deck -> !isGeneratedDifficultDeck(deck))
                .map(deck -> toResponse(deck, owner))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DeckResponse> listDifficultDecks(Authentication authentication) {
        User owner = userService.currentUser(authentication);
        return deckRepository.findAllByOwnerIdOrderByUpdatedAtDesc(owner.getId())
                .stream()
                .filter(this::isGeneratedDifficultDeck)
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
            learnProgressRepository.deleteAllByUserIdAndVocabItemIdIn(owner.getId(), vocabIds);
        }
        learnSessionRepository.deleteAllByUserIdAndDeckId(owner.getId(), deck.getId());
        return toResponse(deck, owner);
    }

    private void apply(Deck deck, DeckRequest request) {
        deck.setName(request.name().trim());
        deck.setDescription(request.description() == null ? null : request.description().trim());
    }

    private VocabItem copyDifficultItem(VocabItem source, Deck deck) {
        VocabItem copy = new VocabItem();
        copy.setDeck(deck);
        copy.setWord(source.getWord());
        copy.setNormalizedWord(source.getNormalizedWord());
        copy.setPartOfSpeech(source.getPartOfSpeech());
        copy.setMeaningVi(source.getMeaningVi());
        copy.setIpa(source.getIpa());
        copy.setPronunciationHint(source.getPronunciationHint());
        copy.setExampleEn(source.getExampleEn());
        copy.setExampleVi(source.getExampleVi());
        copy.setTopic(source.getTopic());
        copy.setLevel(source.getLevel());
        copy.setSynonyms(source.getSynonyms());
        copy.setAntonyms(source.getAntonyms());
        copy.setCollocations(source.getCollocations());
        copy.setEnrichedAt(source.getEnrichedAt());
        copy.setAudioUrl(source.getAudioUrl());
        copy.setAudioUsUrl(source.getAudioUsUrl());
        copy.setAudioUkUrl(source.getAudioUkUrl());
        copy.setAudioAccent(source.getAudioAccent());
        copy.setAudioSource(source.getAudioSource());
        copy.setAudioRefreshedAt(source.getAudioRefreshedAt());
        return copy;
    }

    private String generateDifficultDeckName(Long ownerId) {
        for (int attempt = 0; attempt < 20; attempt++) {
            String name = "difficult_day_%s_%s".formatted(
                    LocalDate.now().format(DIFFICULT_DECK_DAY_FORMAT),
                    UUID.randomUUID().toString().replace("-", "").substring(0, 6)
            );
            if (!deckRepository.existsByOwnerIdAndName(ownerId, name)) {
                return name;
            }
        }
        return "difficult_day_%s_%s".formatted(
                LocalDate.now().format(DIFFICULT_DECK_DAY_FORMAT),
                UUID.randomUUID().toString().replace("-", "")
        );
    }

    private boolean isGeneratedDifficultDeck(Deck deck) {
        String description = deck.getDescription() == null ? "" : deck.getDescription();
        return deck.getName().startsWith("difficult_day_")
                || description.startsWith("Auto-generated from difficult words");
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
        return DeckResponse.from(deck, totalWords, learnedWords, dueWords, dueTodayCount, savedQuestionCount, owner.getId(), isGeneratedDifficultDeck(deck));
    }
}
