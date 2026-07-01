package com.voca.backend.deck;

import com.voca.backend.quiz.Question;
import com.voca.backend.quiz.QuestionRepository;
import com.voca.backend.user.User;
import com.voca.backend.user.UserService;
import com.voca.backend.vocab.VocabItem;
import com.voca.backend.vocab.VocabItemRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class DeckShareService {

    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 10;
    private static final int MAX_CODE_ATTEMPTS = 8;

    private final SecureRandom random = new SecureRandom();

    private final DeckShareCodeRepository shareCodeRepository;
    private final DeckRepository deckRepository;
    private final DeckService deckService;
    private final VocabItemRepository vocabItemRepository;
    private final QuestionRepository questionRepository;
    private final UserService userService;

    public DeckShareService(
            DeckShareCodeRepository shareCodeRepository,
            DeckRepository deckRepository,
            DeckService deckService,
            VocabItemRepository vocabItemRepository,
            QuestionRepository questionRepository,
            UserService userService
    ) {
        this.shareCodeRepository = shareCodeRepository;
        this.deckRepository = deckRepository;
        this.deckService = deckService;
        this.vocabItemRepository = vocabItemRepository;
        this.questionRepository = questionRepository;
        this.userService = userService;
    }

    @Transactional
    public DeckShareCodeResponse getOrCreateShareCode(Authentication authentication, Long deckId) {
        User user = userService.currentUser(authentication);
        Deck deck = deckService.findOwnedDeck(user, deckId);
        DeckShareCode shareCode = shareCodeRepository.findByDeckId(deck.getId())
                .orElseGet(() -> createShareCode(deck));
        return toResponse(shareCode);
    }

    @Transactional
    public DeckShareCodeResponse rotateShareCode(Authentication authentication, Long deckId) {
        User user = userService.currentUser(authentication);
        Deck deck = deckService.findOwnedDeck(user, deckId);
        shareCodeRepository.findByDeckId(deck.getId()).ifPresent(shareCodeRepository::delete);
        shareCodeRepository.flush();
        return toResponse(createShareCode(deck));
    }

    @Transactional(readOnly = true)
    public DeckSharePreviewResponse previewImport(Authentication authentication, DeckShareImportRequest request) {
        userService.currentUser(authentication);
        DeckShareCode shareCode = resolveCode(request);
        Deck sourceDeck = shareCode.getDeck();
        long totalWords = vocabItemRepository.countByDeckId(sourceDeck.getId());
        long totalQuestions = questionRepository.countByDeckIdAndOwnerId(sourceDeck.getId(), sourceDeck.getOwner().getId());
        String ownerName = sourceDeck.getOwner().getDisplayName() != null
                ? sourceDeck.getOwner().getDisplayName()
                : sourceDeck.getOwner().getEmail();
        return new DeckSharePreviewResponse(
                shareCode.getCode(),
                sourceDeck.getName(),
                sourceDeck.getDescription(),
                ownerName,
                Math.toIntExact(totalWords),
                Math.toIntExact(totalQuestions)
        );
    }

    @Transactional
    public DeckResponse importDeck(Authentication authentication, DeckShareImportRequest request) {
        User user = userService.currentUser(authentication);
        DeckShareCode shareCode = resolveCode(request);
        Deck sourceDeck = shareCode.getDeck();

        if (sourceDeck.getOwner().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đây là deck của bạn, không cần import");
        }

        Deck newDeck = new Deck();
        newDeck.setOwner(user);
        newDeck.setName(buildUniqueName(user, sourceDeck.getName()));
        newDeck.setDescription(sourceDeck.getDescription());
        Deck savedDeck = deckRepository.save(newDeck);

        List<VocabItem> sourceItems = vocabItemRepository.findAllByDeckIdOrderByCreatedAtAsc(sourceDeck.getId());
        Map<Long, VocabItem> sourceToCopy = new HashMap<>();
        List<VocabItem> copiedItems = new ArrayList<>(sourceItems.size());
        for (VocabItem source : sourceItems) {
            VocabItem copy = new VocabItem();
            copy.setDeck(savedDeck);
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
            copiedItems.add(copy);
        }
        List<VocabItem> savedItems = vocabItemRepository.saveAll(copiedItems);
        for (int i = 0; i < sourceItems.size(); i++) {
            sourceToCopy.put(sourceItems.get(i).getId(), savedItems.get(i));
        }

        List<Question> sourceQuestions = questionRepository.findAllByDeckIdAndOwnerIdOrderByCreatedAtAsc(
                sourceDeck.getId(),
                sourceDeck.getOwner().getId()
        );
        List<Question> copiedQuestions = new ArrayList<>(sourceQuestions.size());
        for (Question source : sourceQuestions) {
            VocabItem newVocab = sourceToCopy.get(source.getVocabItem().getId());
            if (newVocab == null) {
                continue;
            }
            Question copy = new Question();
            copy.setDeck(savedDeck);
            copy.setOwner(user);
            copy.setVocabItem(newVocab);
            copy.setType(source.getType());
            copy.setPrompt(source.getPrompt());
            copy.setCorrectAnswer(source.getCorrectAnswer());
            copy.setOptionsJson(null);
            copy.setExplanation(source.getExplanation());
            copiedQuestions.add(copy);
        }
        questionRepository.saveAll(copiedQuestions);

        return deckService.toResponse(savedDeck, user);
    }

    private DeckShareCode resolveCode(DeckShareImportRequest request) {
        if (request == null || request.code() == null || request.code().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng nhập mã chia sẻ");
        }
        String normalized = request.code().trim().toUpperCase(Locale.ROOT);
        return shareCodeRepository.findByCode(normalized)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mã chia sẻ không hợp lệ"));
    }

    private DeckShareCode createShareCode(Deck deck) {
        DeckShareCode shareCode = new DeckShareCode();
        shareCode.setDeck(deck);
        shareCode.setCode(generateUniqueCode());
        return shareCodeRepository.save(shareCode);
    }

    private String generateUniqueCode() {
        for (int attempt = 0; attempt < MAX_CODE_ATTEMPTS; attempt++) {
            StringBuilder builder = new StringBuilder(CODE_LENGTH);
            for (int i = 0; i < CODE_LENGTH; i++) {
                builder.append(CODE_ALPHABET.charAt(random.nextInt(CODE_ALPHABET.length())));
            }
            String candidate = builder.toString();
            if (!shareCodeRepository.existsByCode(candidate)) {
                return candidate;
            }
        }
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể tạo mã chia sẻ");
    }

    private String buildUniqueName(User owner, String baseName) {
        String name = baseName == null || baseName.isBlank() ? "Imported deck" : baseName;
        if (!deckRepository.existsByOwnerIdAndName(owner.getId(), name)) {
            return name;
        }
        for (int i = 2; i < 200; i++) {
            String candidate = name + " (" + i + ")";
            if (!deckRepository.existsByOwnerIdAndName(owner.getId(), candidate)) {
                return candidate;
            }
        }
        return name + " (" + System.currentTimeMillis() + ")";
    }

    private DeckShareCodeResponse toResponse(DeckShareCode shareCode) {
        Deck deck = shareCode.getDeck();
        return new DeckShareCodeResponse(deck.getId(), deck.getName(), shareCode.getCode(), shareCode.getCreatedAt());
    }
}
