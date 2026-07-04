package com.voca.backend.review;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.voca.backend.classroom.ClassroomDeckRepository;
import com.voca.backend.deck.Deck;
import com.voca.backend.deck.DeckService;
import com.voca.backend.user.User;
import com.voca.backend.user.UserService;
import com.voca.backend.vocab.UserProgress;
import com.voca.backend.vocab.UserProgressRepository;
import com.voca.backend.vocab.VocabItem;
import com.voca.backend.vocab.VocabItemRepository;
import com.voca.backend.vocab.VocabProgressStatus;

@Service
public class ReviewService {

    private static final int DEFAULT_LIMIT = 20;

    private final UserService userService;
    private final VocabItemRepository vocabItemRepository;
    private final UserProgressRepository userProgressRepository;
    private final ReviewSchedulingService reviewSchedulingService;
    private final DeckService deckService;
    private final ClassroomDeckRepository classroomDeckRepository;

    public ReviewService(
            UserService userService,
            VocabItemRepository vocabItemRepository,
            UserProgressRepository userProgressRepository,
            ReviewSchedulingService reviewSchedulingService,
            DeckService deckService,
            ClassroomDeckRepository classroomDeckRepository
    ) {
        this.userService = userService;
        this.vocabItemRepository = vocabItemRepository;
        this.userProgressRepository = userProgressRepository;
        this.reviewSchedulingService = reviewSchedulingService;
        this.deckService = deckService;
        this.classroomDeckRepository = classroomDeckRepository;
    }

    @Transactional(readOnly = true)
    public ReviewTodayResponse today(Authentication authentication, Long deckId, Integer limit, VocabProgressStatus status) {
        User user = userService.currentUser(authentication);
        LocalDateTime now = LocalDateTime.now();
        int actualLimit = limit == null ? DEFAULT_LIMIT : Math.max(1, limit);
        List<VocabItem> items = loadReviewItems(user, deckId);
        if (items.isEmpty()) {
            return new ReviewTodayResponse(List.of(), 0);
        }

        Map<Long, UserProgress> progressByVocabId = userProgressRepository
                .findAllByUserIdAndVocabItemIdIn(user.getId(), items.stream().map(VocabItem::getId).toList())
                .stream()
                .collect(Collectors.toMap(progress -> progress.getVocabItem().getId(), Function.identity()));

        List<ReviewCandidate> dueItems = items.stream()
                .map(item -> new ReviewCandidate(item, progressByVocabId.get(item.getId())))
                .filter(candidate -> isReviewable(candidate.progress()))
                .filter(candidate -> isDue(candidate.progress(), now))
                .filter(candidate -> status == null || progressStatus(candidate.progress()).equals(status))
                .sorted(reviewComparator(now))
                .toList();

        return new ReviewTodayResponse(
                dueItems.stream()
                        .limit(actualLimit)
                        .map(candidate -> ReviewItemResponse.from(candidate.item(), candidate.progress()))
                        .toList(),
                dueItems.size()
        );
    }

    @Transactional(readOnly = true)
    public ReviewScheduleResponse schedule(Authentication authentication, Long deckId, Integer limit, VocabProgressStatus status) {
        User user = userService.currentUser(authentication);
        LocalDateTime now = LocalDateTime.now();
        int actualLimit = limit == null ? 200 : Math.max(1, limit);
        List<VocabItem> items = loadReviewItems(user, deckId);
        if (items.isEmpty()) {
            return new ReviewScheduleResponse(List.of(), 0, 0, 0, 0, 0);
        }

        Map<Long, UserProgress> progressByVocabId = userProgressRepository
                .findAllByUserIdAndVocabItemIdIn(user.getId(), items.stream().map(VocabItem::getId).toList())
                .stream()
                .collect(Collectors.toMap(progress -> progress.getVocabItem().getId(), Function.identity()));

        List<ReviewCandidate> candidates = items.stream()
                .map(item -> new ReviewCandidate(item, progressByVocabId.get(item.getId())))
                .filter(candidate -> isReviewable(candidate.progress()))
                .filter(candidate -> status == null || progressStatus(candidate.progress()).equals(status))
                .sorted(scheduleComparator(now))
                .toList();

        long dueNow = candidates.stream()
                .filter(candidate -> isDue(candidate.progress(), now))
                .count();
        long overdue = candidates.stream()
                .filter(candidate -> isScheduledOverdue(candidate.progress(), now))
                .count();
        long newItems = candidates.stream()
                .filter(candidate -> candidate.progress() == null || candidate.progress().getNextReviewAt() == null)
                .count();

        return new ReviewScheduleResponse(
                candidates.stream()
                        .limit(actualLimit)
                        .map(candidate -> ReviewScheduleItemResponse.from(candidate.item(), candidate.progress(), now))
                        .toList(),
                candidates.size(),
                dueNow,
                overdue,
                Math.max(0, candidates.size() - dueNow),
                newItems
        );
    }

    @Transactional
    public ReviewProgressResponse submit(Authentication authentication, Long vocabId, ReviewResultRequest request) {
        User user = userService.currentUser(authentication);
        VocabItem item = vocabItemRepository.findById(vocabId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vocabulary item not found"));
        if (!deckService.canStudyDeck(user, item.getDeck())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Vocabulary item not found");
        }
        ReviewQuality quality = request.quality() == null
                ? reviewSchedulingService.infer(request.isCorrect(), request.responseTimeMs())
                : request.quality();

        UserProgress progress = userProgressRepository.findByUserIdAndVocabItemId(user.getId(), item.getId())
                .orElseGet(() -> createProgress(user, item));

        reviewSchedulingService.apply(progress, quality, request.responseTimeMs(), LocalDateTime.now());
        UserProgress saved = userProgressRepository.save(progress);
        return ReviewProgressResponse.from(saved, quality);
    }

    @Transactional
    public UserProgress applyQuizResult(User user, VocabItem item, boolean correct, Integer responseTimeMs) {
        return applyQuizResult(user, item, correct, responseTimeMs, null);
    }

    @Transactional
    public UserProgress applyQuizResult(User user, VocabItem item, boolean correct, Integer responseTimeMs, ReviewQuality qualityOverride) {
        UserProgress progress = userProgressRepository.findByUserIdAndVocabItemId(user.getId(), item.getId())
                .orElseGet(() -> createProgress(user, item));
        ReviewQuality quality = qualityOverride != null
                ? qualityOverride
                : reviewSchedulingService.infer(correct, responseTimeMs);
        reviewSchedulingService.apply(progress, quality, responseTimeMs, LocalDateTime.now());
        return userProgressRepository.save(progress);
    }

    @Transactional
    public UserProgress applyLearnResult(
            User user,
            VocabItem item,
            int correctAttempts,
            int incorrectAttempts,
            Integer responseTimeMs,
            ReviewQuality qualityOverride
    ) {
        UserProgress progress = userProgressRepository.findByUserIdAndVocabItemId(user.getId(), item.getId())
                .orElseGet(() -> createProgress(user, item));
        LocalDateTime now = LocalDateTime.now();
        ReviewQuality quality = learnQuality(incorrectAttempts, qualityOverride);

        if (incorrectAttempts == 1 && quality != ReviewQuality.AGAIN) {
            progress.setWrongCount(progress.getWrongCount() + 1);
            progress.setLapseCount(progress.getLapseCount() + 1);
            progress.incrementUnknownCount();
        }

        reviewSchedulingService.apply(progress, quality, responseTimeMs, now);

        if (incorrectAttempts >= 2) {
            int extraWrongs = incorrectAttempts - 1;
            progress.setWrongCount(progress.getWrongCount() + extraWrongs);
            progress.setLapseCount(progress.getLapseCount() + extraWrongs);
            progress.setUnknownCount(progress.getUnknownCount() + extraWrongs);
            progress.incrementDifficultCount();
            progress.setStatus(VocabProgressStatus.DIFFICULT);
        }

        return userProgressRepository.save(progress);
    }

    private ReviewQuality learnQuality(int incorrectAttempts, ReviewQuality qualityOverride) {
        if (incorrectAttempts >= 2) {
            return ReviewQuality.AGAIN;
        }
        if (incorrectAttempts == 1) {
            return qualityOverride == ReviewQuality.AGAIN ? ReviewQuality.AGAIN : ReviewQuality.HARD;
        }
        return qualityOverride == null ? ReviewQuality.GOOD : qualityOverride;
    }

    private List<VocabItem> loadReviewItems(User user, Long deckId) {
        if (deckId != null) {
            Deck deck = deckService.findStudyDeck(user, deckId);
            return vocabItemRepository.findAllByDeckIdOrderByCreatedAtAsc(deck.getId());
        }

        List<VocabItem> ownedItems = vocabItemRepository.findAllByDeckOwnerIdOrderByCreatedAtAsc(user.getId());
        List<Long> classDeckIds = classroomDeckRepository.findStudyDeckIds(user.getId());
        List<VocabItem> classItems = classDeckIds.isEmpty()
                ? List.of()
                : vocabItemRepository.findAllByDeckIdIn(classDeckIds);

        return Stream.concat(ownedItems.stream(), classItems.stream())
                .collect(Collectors.toMap(VocabItem::getId, Function.identity(), (left, right) -> left, LinkedHashMap::new))
                .values()
                .stream()
                .toList();
    }

    private UserProgress createProgress(User user, VocabItem item) {
        UserProgress progress = new UserProgress();
        progress.setUser(user);
        progress.setVocabItem(item);
        return progress;
    }

    private boolean isDue(UserProgress progress, LocalDateTime now) {
        if (!isReviewable(progress)) {
            return false;
        }
        return progress.getNextReviewAt() == null || !progress.getNextReviewAt().isAfter(now);
    }

    private boolean isReviewable(UserProgress progress) {
        return progress != null
                && progress.getStatus() != VocabProgressStatus.NEW
                && progress.getNextReviewAt() != null;
    }

    private VocabProgressStatus progressStatus(UserProgress progress) {
        return progress == null ? VocabProgressStatus.NEW : progress.getStatus();
    }

    private Comparator<ReviewCandidate> reviewComparator(LocalDateTime now) {
        return Comparator
                .comparing((ReviewCandidate candidate) -> !isOverdue(candidate.progress(), now))
                .thenComparing(candidate -> !progressStatus(candidate.progress()).equals(VocabProgressStatus.DIFFICULT))
                .thenComparing(candidate -> candidate.progress() == null || candidate.progress().getNextReviewAt() == null
                        ? LocalDateTime.MIN
                        : candidate.progress().getNextReviewAt());
    }

    private Comparator<ReviewCandidate> scheduleComparator(LocalDateTime now) {
        return Comparator
                .comparingInt((ReviewCandidate candidate) -> schedulePriority(scheduleBucket(candidate.progress(), now)))
                .thenComparing(candidate -> candidate.progress() == null || candidate.progress().getNextReviewAt() == null
                        ? LocalDateTime.MIN
                        : candidate.progress().getNextReviewAt())
                .thenComparing(candidate -> candidate.item().getWord().toLowerCase());
    }

    private boolean isOverdue(UserProgress progress, LocalDateTime now) {
        return isReviewable(progress) && progress.getNextReviewAt().isBefore(now);
    }

    private boolean isScheduledOverdue(UserProgress progress, LocalDateTime now) {
        return progress != null && progress.getNextReviewAt() != null && progress.getNextReviewAt().isBefore(now);
    }

    static ReviewScheduleBucket scheduleBucket(UserProgress progress, LocalDateTime now) {
        if (progress == null || progress.getNextReviewAt() == null) {
            return ReviewScheduleBucket.NEW;
        }
        LocalDateTime nextReviewAt = progress.getNextReviewAt();
        if (nextReviewAt.isBefore(now)) {
            return ReviewScheduleBucket.OVERDUE;
        }
        if (!nextReviewAt.isAfter(now.plusMinutes(1))) {
            return ReviewScheduleBucket.DUE_NOW;
        }
        if (nextReviewAt.toLocalDate().isEqual(now.toLocalDate())) {
            return ReviewScheduleBucket.TODAY;
        }
        if (nextReviewAt.toLocalDate().isEqual(now.toLocalDate().plusDays(1))) {
            return ReviewScheduleBucket.TOMORROW;
        }
        if (!nextReviewAt.isAfter(now.plusDays(7))) {
            return ReviewScheduleBucket.THIS_WEEK;
        }
        return ReviewScheduleBucket.LATER;
    }

    private int schedulePriority(ReviewScheduleBucket bucket) {
        return switch (bucket) {
            case OVERDUE -> 0;
            case DUE_NOW -> 1;
            case NEW -> 2;
            case TODAY -> 3;
            case TOMORROW -> 4;
            case THIS_WEEK -> 5;
            case LATER -> 6;
        };
    }

    private record ReviewCandidate(VocabItem item, UserProgress progress) {
    }
}
