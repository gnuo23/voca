package com.voca.backend.vocab;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface VocabItemRepository extends JpaRepository<VocabItem, Long> {

    boolean existsByDeckIdAndNormalizedWord(Long deckId, String normalizedWord);

    List<VocabItem> findAllByDeckIdAndNormalizedWordIn(Long deckId, Collection<String> normalizedWords);

    long countByDeckId(Long deckId);
}
