package com.voca.backend.dashboard;

import com.voca.backend.deck.Deck;
import com.voca.backend.deck.DeckRepository;
import com.voca.backend.user.User;
import com.voca.backend.user.UserService;
import com.voca.backend.vocab.UserProgress;
import com.voca.backend.vocab.UserProgressRepository;
import com.voca.backend.vocab.VocabItem;
import com.voca.backend.vocab.VocabItemRepository;
import com.voca.backend.vocab.VocabProgressStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final UserService userService;
    private final DeckRepository deckRepository;
    private final VocabItemRepository vocabItemRepository;
    private final UserProgressRepository userProgressRepository;

    public DashboardService(
            UserService userService,
            DeckRepository deckRepository,
            VocabItemRepository vocabItemRepository,
            UserProgressRepository userProgressRepository
    ) {
        this.userService = userService;
        this.deckRepository = deckRepository;
        this.vocabItemRepository = vocabItemRepository;
        this.userProgressRepository = userProgressRepository;
    }

    @Transactional(readOnly = true)
    public DashboardResponse get(Authentication authentication) {
        User user = userService.currentUser(authentication);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        LocalDateTime startOfTomorrow = startOfToday.plusDays(1);
        List<UserProgress> allProgress = userProgressRepository.findAllByUserId(user.getId());
        List<Deck> decks = deckRepository.findAllByOwnerIdOrderByUpdatedAtDesc(user.getId());
        List<VocabItem> allItems = vocabItemRepository.findAllByDeckOwnerIdOrderByCreatedAtAsc(user.getId());

        long correct = allProgress.stream().mapToLong(UserProgress::getCorrectCount).sum();
        long wrong = allProgress.stream().mapToLong(UserProgress::getWrongCount).sum();
        double accuracy = correct + wrong == 0 ? 0 : Math.round((correct * 1000.0) / (correct + wrong)) / 10.0;

        return new DashboardResponse(
                userProgressRepository.countByUserIdAndCreatedAtBetweenAndLastReviewedAtIsNotNull(user.getId(), startOfToday, startOfTomorrow),
                userProgressRepository.countByUserIdAndLastReviewedAtBetween(user.getId(), startOfToday, startOfTomorrow),
                userProgressRepository.countByUserIdAndNextReviewAtLessThanEqual(user.getId(), now),
                userProgressRepository.countByUserIdAndNextReviewAtLessThan(user.getId(), startOfToday),
                accuracy,
                streakDays(allProgress),
                hardWords(allProgress),
                deckProgress(decks, allItems, allProgress)
        );
    }

    private List<HardWordResponse> hardWords(List<UserProgress> progress) {
        return progress.stream()
                .filter(item -> item.getStatus() == VocabProgressStatus.DIFFICULT || item.getLapseCount() >= 3 || item.getWrongCount() >= 3)
                .sorted(Comparator.comparing(UserProgress::getWrongCount).reversed()
                        .thenComparing(Comparator.comparing(UserProgress::getLapseCount).reversed()))
                .limit(10)
                .map(item -> new HardWordResponse(
                        item.getVocabItem().getId(),
                        item.getVocabItem().getWord(),
                        item.getVocabItem().getMeaningVi(),
                        item.getWrongCount(),
                        item.getLapseCount(),
                        item.getStatus()
                ))
                .toList();
    }

    private List<DeckProgressResponse> deckProgress(List<Deck> decks, List<VocabItem> items, List<UserProgress> progress) {
        Map<Long, UserProgress> progressByVocabId = progress.stream()
                .collect(Collectors.toMap(item -> item.getVocabItem().getId(), Function.identity()));
        Map<Long, List<VocabItem>> itemsByDeckId = items.stream()
                .collect(Collectors.groupingBy(item -> item.getDeck().getId()));

        return decks.stream()
                .map(deck -> {
                    List<VocabItem> deckItems = itemsByDeckId.getOrDefault(deck.getId(), List.of());
                    long totalWords = deckItems.size();
                    long learning = countStatus(deckItems, progressByVocabId, VocabProgressStatus.LEARNING);
                    long review = countStatus(deckItems, progressByVocabId, VocabProgressStatus.REVIEW);
                    long difficult = countStatus(deckItems, progressByVocabId, VocabProgressStatus.DIFFICULT);
                    long mastered = countStatus(deckItems, progressByVocabId, VocabProgressStatus.MASTERED);
                    long explicitNew = countStatus(deckItems, progressByVocabId, VocabProgressStatus.NEW);
                    long missingProgress = deckItems.stream().filter(item -> !progressByVocabId.containsKey(item.getId())).count();
                    long newCount = explicitNew + missingProgress;
                    double score = totalWords == 0 ? 0 : ((mastered + review * 0.7 + learning * 0.4 + difficult * 0.2) / totalWords) * 100;

                    return new DeckProgressResponse(
                            deck.getId(),
                            deck.getName(),
                            totalWords,
                            newCount,
                            learning,
                            review,
                            difficult,
                            mastered,
                            Math.round(score * 100.0) / 100.0
                    );
                })
                .toList();
    }

    private long countStatus(List<VocabItem> items, Map<Long, UserProgress> progressByVocabId, VocabProgressStatus status) {
        return items.stream()
                .map(item -> progressByVocabId.get(item.getId()))
                .filter(progress -> progress != null && progress.getStatus() == status)
                .count();
    }

    private int streakDays(List<UserProgress> progress) {
        Set<LocalDate> dates = progress.stream()
                .map(UserProgress::getLastReviewedAt)
                .filter(reviewedAt -> reviewedAt != null)
                .map(LocalDateTime::toLocalDate)
                .collect(Collectors.toSet());

        int streak = 0;
        LocalDate cursor = LocalDate.now();
        while (dates.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }
}
