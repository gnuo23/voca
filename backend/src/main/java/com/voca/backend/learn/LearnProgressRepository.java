package com.voca.backend.learn;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LearnProgressRepository extends JpaRepository<LearnProgress, Long> {

    Optional<LearnProgress> findByUserIdAndVocabItemId(Long userId, Long vocabItemId);

    List<LearnProgress> findAllByUserIdAndVocabItemIdIn(Long userId, Collection<Long> vocabItemIds);

    long countByUserIdAndVocabItemIdInAndMasteredTrue(Long userId, Collection<Long> vocabItemIds);

    void deleteAllByUserIdAndVocabItemIdIn(Long userId, Collection<Long> vocabItemIds);
}
