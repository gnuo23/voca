package com.voca.backend.vocab;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserProgressRepository extends JpaRepository<UserProgress, Long> {

    Optional<UserProgress> findByUserIdAndVocabItemId(Long userId, Long vocabItemId);

    List<UserProgress> findAllByUserIdAndVocabItemIdIn(Long userId, Collection<Long> vocabItemIds);

    long countByUserIdAndVocabItemDeckIdAndStatus(Long userId, Long deckId, VocabProgressStatus status);

    long countByUserIdAndStatusIn(Long userId, Collection<VocabProgressStatus> statuses);

    long countByUserIdAndNextReviewAtLessThanEqual(Long userId, LocalDateTime now);

    long countByUserIdAndNextReviewAtLessThan(Long userId, LocalDateTime dateTime);

    long countByUserIdAndLastReviewedAtBetween(Long userId, LocalDateTime start, LocalDateTime end);

    long countByUserIdAndCreatedAtBetweenAndLastReviewedAtIsNotNull(Long userId, LocalDateTime start, LocalDateTime end);

    List<UserProgress> findAllByUserId(Long userId);

    List<UserProgress> findAllByVocabItemId(Long vocabItemId);

    void deleteAllByVocabItemId(Long vocabItemId);

}
