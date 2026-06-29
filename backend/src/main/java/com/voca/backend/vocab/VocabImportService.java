package com.voca.backend.vocab;

import com.voca.backend.deck.Deck;
import com.voca.backend.deck.DeckService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class VocabImportService {

    private final VocabListParser parser;
    private final VocabItemRepository vocabItemRepository;
    private final DeckService deckService;

    public VocabImportService(
            VocabListParser parser,
            VocabItemRepository vocabItemRepository,
            DeckService deckService
    ) {
        this.parser = parser;
        this.vocabItemRepository = vocabItemRepository;
        this.deckService = deckService;
    }

    @Transactional(readOnly = true)
    public VocabImportPreviewResponse preview(Authentication authentication, VocabImportRequest request) {
        Deck deck = deckService.findOwnedDeck(authentication, request.deckId());
        return buildPreview(deck.getId(), request.rawText());
    }

    @Transactional
    public VocabImportConfirmResponse confirm(Authentication authentication, VocabImportRequest request) {
        Deck deck = deckService.findOwnedDeck(authentication, request.deckId());
        VocabImportPreviewResponse preview = buildPreview(deck.getId(), request.rawText());

        if (preview.hasBlockingErrors()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Import contains errors or duplicates");
        }

        List<VocabItem> items = preview.items().stream()
                .map(item -> toEntity(deck, item))
                .toList();

        vocabItemRepository.saveAll(items);
        return new VocabImportConfirmResponse(items.size(), preview.items(), preview.errors());
    }

    private VocabImportPreviewResponse buildPreview(Long deckId, String rawText) {
        List<ParsedVocabLine> parsedLines = parser.parse(rawText);
        List<VocabImportErrorResponse> errors = new ArrayList<>();

        if (parsedLines.isEmpty()) {
            errors.add(new VocabImportErrorResponse(1, "No vocabulary lines found"));
            return new VocabImportPreviewResponse(List.of(), errors);
        }

        List<ParsedVocabLine> validLines = new ArrayList<>();
        for (ParsedVocabLine parsedLine : parsedLines) {
            if (parsedLine.isValid()) {
                validLines.add(parsedLine);
            } else {
                errors.add(new VocabImportErrorResponse(parsedLine.lineNumber(), parsedLine.error()));
            }
        }

        Set<String> normalizedWords = validLines.stream()
                .map(line -> normalizeWord(line.word()))
                .collect(Collectors.toSet());
        Map<String, VocabItem> existingByWord = normalizedWords.isEmpty()
                ? Map.of()
                : vocabItemRepository
                        .findAllByDeckIdAndNormalizedWordIn(deckId, normalizedWords)
                        .stream()
                        .collect(Collectors.toMap(VocabItem::getNormalizedWord, Function.identity()));
        Set<String> seenInImport = new HashSet<>();
        Map<String, Integer> firstLineByWord = new HashMap<>();

        List<VocabImportItemResponse> items = validLines.stream()
                .map(line -> toPreviewItem(line, existingByWord, seenInImport, firstLineByWord))
                .toList();

        return new VocabImportPreviewResponse(items, errors);
    }

    private VocabImportItemResponse toPreviewItem(
            ParsedVocabLine line,
            Map<String, VocabItem> existingByWord,
            Set<String> seenInImport,
            Map<String, Integer> firstLineByWord
    ) {
        String normalizedWord = normalizeWord(line.word());

        if (existingByWord.containsKey(normalizedWord)) {
            return new VocabImportItemResponse(
                    line.lineNumber(),
                    line.word(),
                    line.partOfSpeech(),
                    line.meaningVi(),
                    VocabImportStatus.DUPLICATE_IN_DECK,
                    "Word already exists in this deck"
            );
        }

        if (!seenInImport.add(normalizedWord)) {
            return new VocabImportItemResponse(
                    line.lineNumber(),
                    line.word(),
                    line.partOfSpeech(),
                    line.meaningVi(),
                    VocabImportStatus.DUPLICATE_IN_IMPORT,
                    "Duplicate of line " + firstLineByWord.get(normalizedWord)
            );
        }

        firstLineByWord.put(normalizedWord, line.lineNumber());
        return new VocabImportItemResponse(
                line.lineNumber(),
                line.word(),
                line.partOfSpeech(),
                line.meaningVi(),
                VocabImportStatus.OK,
                null
        );
    }

    private VocabItem toEntity(Deck deck, VocabImportItemResponse item) {
        VocabItem entity = new VocabItem();
        entity.setDeck(deck);
        entity.setWord(item.word());
        entity.setNormalizedWord(normalizeWord(item.word()));
        entity.setPartOfSpeech(item.partOfSpeech());
        entity.setMeaningVi(item.meaningVi());
        return entity;
    }

    private String normalizeWord(String word) {
        return word.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
