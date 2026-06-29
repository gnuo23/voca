package com.voca.backend.vocab;

import com.voca.backend.deck.Deck;
import com.voca.backend.deck.DeckService;
import com.voca.backend.review.ReviewQuality;
import com.voca.backend.review.ReviewSchedulingService;
import com.voca.backend.user.User;
import com.voca.backend.user.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class VocabItemService {

    private final VocabItemRepository vocabItemRepository;
    private final UserProgressRepository userProgressRepository;
    private final DeckService deckService;
    private final UserService userService;
    private final DictionaryAudioClient dictionaryAudioClient;
    private final ReviewSchedulingService reviewSchedulingService;

    public VocabItemService(
            VocabItemRepository vocabItemRepository,
            UserProgressRepository userProgressRepository,
            DeckService deckService,
            UserService userService,
            DictionaryAudioClient dictionaryAudioClient,
            ReviewSchedulingService reviewSchedulingService
    ) {
        this.vocabItemRepository = vocabItemRepository;
        this.userProgressRepository = userProgressRepository;
        this.deckService = deckService;
        this.userService = userService;
        this.dictionaryAudioClient = dictionaryAudioClient;
        this.reviewSchedulingService = reviewSchedulingService;
    }

    @Transactional(readOnly = true)
    public List<VocabItemResponse> listByDeck(Authentication authentication, Long deckId) {
        User user = userService.currentUser(authentication);
        Deck deck = deckService.findOwnedDeck(authentication, deckId);
        List<VocabItem> items = vocabItemRepository.findAllByDeckIdOrderByCreatedAtAsc(deck.getId());
        Map<Long, UserProgress> progressByVocabId = userProgressRepository
                .findAllByUserIdAndVocabItemIdIn(
                        user.getId(),
                        items.stream().map(VocabItem::getId).toList()
                )
                .stream()
                .collect(Collectors.toMap(progress -> progress.getVocabItem().getId(), Function.identity()));

        return items.stream()
                .map(item -> VocabItemResponse.from(item, progressByVocabId.get(item.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public VocabItemResponse get(Authentication authentication, Long vocabId) {
        User user = userService.currentUser(authentication);
        VocabItem item = findOwnedVocab(user, vocabId);
        UserProgress progress = userProgressRepository
                .findByUserIdAndVocabItemId(user.getId(), item.getId())
                .orElse(null);
        return VocabItemResponse.from(item, progress);
    }

    @Transactional
    public VocabItemResponse update(Authentication authentication, Long vocabId, VocabItemRequest request) {
        User user = userService.currentUser(authentication);
        VocabItem item = findOwnedVocab(user, vocabId);
        apply(item, request);

        UserProgress progress = userProgressRepository
                .findByUserIdAndVocabItemId(user.getId(), item.getId())
                .orElse(null);
        return VocabItemResponse.from(item, progress);
    }

    @Transactional
    public VocabAudioResponse getAudio(Authentication authentication, Long vocabId) {
        User user = userService.currentUser(authentication);
        VocabItem item = findOwnedVocab(user, vocabId);
        if (item.getAudioRefreshedAt() == null) {
            refreshAudio(item);
        }
        return VocabAudioResponse.from(item);
    }

    @Transactional
    public VocabAudioResponse refreshAudio(Authentication authentication, Long vocabId) {
        User user = userService.currentUser(authentication);
        VocabItem item = findOwnedVocab(user, vocabId);
        refreshAudio(item);
        return VocabAudioResponse.from(item);
    }

    @Transactional
    public void delete(Authentication authentication, Long vocabId) {
        User user = userService.currentUser(authentication);
        VocabItem item = findOwnedVocab(user, vocabId);
        vocabItemRepository.delete(item);
    }

    @Transactional
    public VocabItemResponse mark(Authentication authentication, Long vocabId, VocabMarkRequest request) {
        User user = userService.currentUser(authentication);
        VocabItem item = findOwnedVocab(user, vocabId);
        UserProgress progress = userProgressRepository
                .findByUserIdAndVocabItemId(user.getId(), item.getId())
                .orElseGet(() -> createProgress(user, item));

        reviewSchedulingService.apply(progress, qualityFor(request.action()), null, LocalDateTime.now());
        userProgressRepository.save(progress);
        return VocabItemResponse.from(item, progress);
    }

    private ReviewQuality qualityFor(VocabMarkAction action) {
        return switch (action) {
            case KNOWN -> ReviewQuality.GOOD;
            case UNKNOWN -> ReviewQuality.AGAIN;
            case DIFFICULT -> ReviewQuality.HARD;
        };
    }

    private VocabItem findOwnedVocab(User user, Long vocabId) {
        return vocabItemRepository.findByIdAndDeckOwnerId(vocabId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vocabulary item not found"));
    }

    private void apply(VocabItem item, VocabItemRequest request) {
        String word = request.word().trim().replaceAll("\\s+", " ");
        String normalizedWord = normalizeWord(word);
        if (vocabItemRepository.existsByDeckIdAndNormalizedWordAndIdNot(
                item.getDeck().getId(),
                normalizedWord,
                item.getId()
        )) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Word already exists in this deck");
        }

        item.setWord(word);
        item.setNormalizedWord(normalizedWord);
        item.setPartOfSpeech(cleanOptional(request.partOfSpeech()));
        item.setMeaningVi(cleanOptional(request.meaningVi()));
    }

    private void refreshAudio(VocabItem item) {
        try {
            DictionaryAudioClient.AudioLookupResult result = dictionaryAudioClient.lookup(item.getWord());
            item.setAudioUrl(result.audioUrl());
            item.setAudioUsUrl(result.audioUsUrl());
            item.setAudioUkUrl(result.audioUkUrl());
            item.setAudioAccent(result.audioAccent());
            item.setAudioSource(result.audioUrl() == null ? null : "Free Dictionary API");
        } catch (DictionaryAudioClient.AudioNotFoundException ex) {
            item.setAudioUrl(null);
            item.setAudioUsUrl(null);
            item.setAudioUkUrl(null);
            item.setAudioAccent(null);
            item.setAudioSource(null);
        }
        item.setAudioRefreshedAt(LocalDateTime.now());
    }

    private UserProgress createProgress(User user, VocabItem item) {
        UserProgress progress = new UserProgress();
        progress.setUser(user);
        progress.setVocabItem(item);
        return progress;
    }

    private String cleanOptional(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private String normalizeWord(String word) {
        return word.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
