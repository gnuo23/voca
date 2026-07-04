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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private static final Set<VocabProgressStatus> LEARNED_STATUSES = Set.of(
            VocabProgressStatus.LEARNING,
            VocabProgressStatus.REVIEW,
            VocabProgressStatus.DIFFICULT,
            VocabProgressStatus.MASTERED
    );

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
        Set<LocalDate> activityDates = activityDates(allProgress);
        LocalDate today = LocalDate.now();
        int streakDays = streakDays(activityDates);

        return new DashboardResponse(
                allProgress.stream().filter(progress -> isLearnedToday(progress, startOfToday, startOfTomorrow)).count(),
                userProgressRepository.countByUserIdAndLastReviewedAtBetween(user.getId(), startOfToday, startOfTomorrow),
                allProgress.stream().filter(progress -> isReviewable(progress) && !progress.getNextReviewAt().isAfter(now)).count(),
                allProgress.stream().filter(progress -> isReviewable(progress) && progress.getNextReviewAt().isBefore(startOfToday)).count(),
                accuracy,
                learningLevel(allProgress, streakDays),
                streakDays,
                activityDates.contains(today),
                streakWeek(activityDates, today),
                weeklyStats(allProgress, today),
                recentActivities(decks, allProgress),
                hardWords(allProgress),
                deckProgress(decks, allItems, allProgress)
        );
    }

    private boolean isLearnedToday(UserProgress progress, LocalDateTime startOfToday, LocalDateTime startOfTomorrow) {
        if (!isLearned(progress)) {
            return false;
        }
        LocalDateTime learnedAt = progress.getLastReviewedAt() != null ? progress.getLastReviewedAt() : progress.getLastMarkedAt();
        return learnedAt != null && !learnedAt.isBefore(startOfToday) && learnedAt.isBefore(startOfTomorrow);
    }

    private boolean isLearned(UserProgress progress) {
        return progress != null && LEARNED_STATUSES.contains(progress.getStatus());
    }

    private boolean isReviewable(UserProgress progress) {
        return isLearned(progress) && progress.getNextReviewAt() != null;
    }

    private LearningLevelResponse learningLevel(List<UserProgress> progress, int streakDays) {
        long mastered = progress.stream().filter(item -> item.getStatus() == VocabProgressStatus.MASTERED).count();
        long review = progress.stream().filter(item -> item.getStatus() == VocabProgressStatus.REVIEW).count();
        long learning = progress.stream().filter(item -> item.getStatus() == VocabProgressStatus.LEARNING).count();
        long difficult = progress.stream().filter(item -> item.getStatus() == VocabProgressStatus.DIFFICULT).count();
        long correct = progress.stream().mapToLong(UserProgress::getCorrectCount).sum();
        long xp = mastered * 40 + review * 25 + learning * 12 + difficult * 8 + correct * 6L + streakDays * 20L;

        int level = 1;
        while (xpForLevel(level + 1) <= xp) {
            level++;
        }
        long current = xpForLevel(level);
        long next = xpForLevel(level + 1);
        int percent = next == current ? 100 : (int) Math.min(100, Math.round(((xp - current) * 100.0) / (next - current)));

        return new LearningLevelResponse(level, xp, current, next, percent);
    }

    private long xpForLevel(int level) {
        long previousLevels = Math.max(0, level - 1L);
        return previousLevels * previousLevels * 120L;
    }

    private List<WeeklyStatResponse> weeklyStats(List<UserProgress> progress, LocalDate today) {
        LocalDate monday = today.minusDays(today.getDayOfWeek().getValue() - 1L);
        String[] labels = {"T2", "T3", "T4", "T5", "T6", "T7", "CN"};

        return java.util.stream.IntStream.range(0, 7)
                .mapToObj(index -> {
                    LocalDate date = monday.plusDays(index);
                    long learned = progress.stream()
                            .filter(this::isLearned)
                            .filter(item -> activityDate(item) != null && activityDate(item).equals(date))
                            .count();
                    long reviewed = progress.stream()
                            .filter(item -> item.getLastReviewedAt() != null && item.getLastReviewedAt().toLocalDate().equals(date))
                            .count();
                    return new WeeklyStatResponse(labels[index], date, learned, reviewed, Math.max(learned, reviewed));
                })
                .toList();
    }

    private List<RecentActivityResponse> recentActivities(List<Deck> decks, List<UserProgress> progress) {
        List<RecentActivityResponse> activities = new ArrayList<>();
        decks.forEach(deck -> {
            activities.add(new RecentActivityResponse(
                    "DECK_CREATED",
                    "Tạo deck mới",
                    deck.getName(),
                    deck.getId(),
                    null,
                    deck.getCreatedAt()
            ));
            if (deck.getUpdatedAt() != null && deck.getCreatedAt() != null && deck.getUpdatedAt().isAfter(deck.getCreatedAt().plusSeconds(5))) {
                activities.add(new RecentActivityResponse(
                        "DECK_UPDATED",
                        "Cập nhật deck",
                        deck.getName(),
                        deck.getId(),
                        null,
                        deck.getUpdatedAt()
                ));
            }
        });

        progress.stream()
                .filter(this::isLearned)
                .filter(item -> activityDateTime(item) != null)
                .forEach(item -> {
                    VocabItem vocab = item.getVocabItem();
                    String title = item.getWrongCount() > 0 || item.getStatus() == VocabProgressStatus.DIFFICULT
                            ? "Luyện từ khó"
                            : "Học từ vựng";
                    String type = item.getWrongCount() > 0 || item.getStatus() == VocabProgressStatus.DIFFICULT
                            ? "HARD_WORD"
                            : "VOCAB_REVIEWED";
                    activities.add(new RecentActivityResponse(
                            type,
                            title,
                            vocab.getWord(),
                            vocab.getDeck().getId(),
                            vocab.getId(),
                            activityDateTime(item)
                    ));
                });

        Set<String> seen = new HashSet<>();
        return activities.stream()
                .filter(item -> item.occurredAt() != null)
                .sorted(Comparator.comparing(RecentActivityResponse::occurredAt).reversed())
                .filter(item -> seen.add(item.type() + ":" + item.deckId() + ":" + item.vocabId() + ":" + item.occurredAt().toLocalDate()))
                .limit(20)
                .toList();
    }

    private LocalDate activityDate(UserProgress progress) {
        LocalDateTime dateTime = activityDateTime(progress);
        return dateTime == null ? null : dateTime.toLocalDate();
    }

    private LocalDateTime activityDateTime(UserProgress progress) {
        return progress.getLastReviewedAt() != null ? progress.getLastReviewedAt() : progress.getLastMarkedAt();
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

    private Set<LocalDate> activityDates(List<UserProgress> progress) {
        return progress.stream()
                .filter(this::isLearned)
                .map(this::activityDateTime)
                .filter(reviewedAt -> reviewedAt != null)
                .map(LocalDateTime::toLocalDate)
                .collect(Collectors.toSet());
    }

    private int streakDays(Set<LocalDate> dates) {
        int streak = 0;
        LocalDate cursor = LocalDate.now();
        while (dates.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }

    private List<StreakDayResponse> streakWeek(Set<LocalDate> dates, LocalDate today) {
        LocalDate monday = today.minusDays(today.getDayOfWeek().getValue() - 1L);
        String[] labels = {"T2", "T3", "T4", "T5", "T6", "T7", "CN"};
        return java.util.stream.IntStream.range(0, 7)
                .mapToObj(index -> {
                    LocalDate date = monday.plusDays(index);
                    return new StreakDayResponse(
                            labels[index],
                            date,
                            dates.contains(date),
                            date.isEqual(today)
                    );
                })
                .toList();
    }
}
