package com.voca.backend.toeic;

import com.voca.backend.user.User;
import com.voca.backend.user.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ToeicDashboardService {

    private static final String COMPLETED = "COMPLETED";

    private final UserService userService;
    private final ToeicAttemptRepository attemptRepository;
    private final ToeicAttemptAnswerRepository answerRepository;

    public ToeicDashboardService(
            UserService userService,
            ToeicAttemptRepository attemptRepository,
            ToeicAttemptAnswerRepository answerRepository
    ) {
        this.userService = userService;
        this.attemptRepository = attemptRepository;
        this.answerRepository = answerRepository;
    }

    @Transactional(readOnly = true)
    public ToeicDashboardResponse getDashboard(Authentication authentication) {
        User user = userService.currentUser(authentication);
        List<ToeicAttempt> attempts = attemptRepository.findAllByUserIdAndStatusOrderByCompletedAtDesc(user.getId(), COMPLETED);
        List<ToeicAttemptAnswerRepository.PartStats> stats = answerRepository.findPartStats(user.getId(), COMPLETED);

        long totalAnswered = stats.stream().mapToLong(ToeicAttemptAnswerRepository.PartStats::getAnswered).sum();
        long totalCorrect = stats.stream().mapToLong(ToeicAttemptAnswerRepository.PartStats::getCorrect).sum();
        LocalDate today = LocalDate.now();
        long completedToday = attempts.stream()
                .filter(attempt -> attempt.getCompletedAt() != null && attempt.getCompletedAt().toLocalDate().equals(today))
                .count();

        // Attempts store the final score; using their total/correct counts makes the daily goal useful even before answer aggregation.
        long answeredToday = attempts.stream()
                .filter(attempt -> attempt.getCompletedAt() != null && attempt.getCompletedAt().toLocalDate().equals(today))
                .mapToLong(ToeicAttempt::getTotalQuestions)
                .sum();
        long correctToday = attempts.stream()
                .filter(attempt -> attempt.getCompletedAt() != null && attempt.getCompletedAt().toLocalDate().equals(today))
                .mapToLong(ToeicAttempt::getCorrectCount)
                .sum();

        List<ToeicPartProgressResponse> progress = stats.stream()
                .map(stat -> new ToeicPartProgressResponse(
                        stat.getPart(),
                        stat.getAnswered(),
                        stat.getCorrect(),
                        percent(stat.getCorrect(), stat.getAnswered())
                ))
                .toList();
        List<ToeicRecentAttemptResponse> recent = attempts.stream()
                .limit(5)
                .map(attempt -> new ToeicRecentAttemptResponse(
                        attempt.getId(),
                        attempt.getTest().getSlug(),
                        attempt.getTest().getTestName(),
                        attempt.getMode(),
                        attempt.getPartFilter(),
                        attempt.getTotalQuestions(),
                        attempt.getCorrectCount(),
                        attempt.getScaledScore(),
                        attempt.getCompletedAt()
                ))
                .toList();

        return new ToeicDashboardResponse(
                percent(totalCorrect, totalAnswered),
                streakDays(attempts),
                attempts.size(),
                totalAnswered,
                totalCorrect,
                answeredToday,
                completedToday,
                Math.max(0, answeredToday - correctToday),
                progress,
                recent
        );
    }

    private int percent(long numerator, long denominator) {
        return denominator == 0 ? 0 : Math.round(numerator * 100f / denominator);
    }

    private int streakDays(List<ToeicAttempt> attempts) {
        Set<LocalDate> days = new HashSet<>();
        attempts.stream()
                .map(ToeicAttempt::getCompletedAt)
                .filter(java.util.Objects::nonNull)
                .map(java.time.LocalDateTime::toLocalDate)
                .forEach(days::add);
        LocalDate cursor = LocalDate.now();
        if (!days.contains(cursor)) {
            cursor = cursor.minusDays(1);
        }
        int streak = 0;
        while (days.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }
}
