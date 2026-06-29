package com.voca.backend.vocab;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface VocabItemRepository extends JpaRepository<VocabItem, Long> {

    boolean existsByDeckIdAndNormalizedWord(Long deckId, String normalizedWord);

    boolean existsByDeckIdAndNormalizedWordAndIdNot(Long deckId, String normalizedWord, Long id);

    List<VocabItem> findAllByDeckIdAndNormalizedWordIn(Long deckId, Collection<String> normalizedWords);

    List<VocabItem> findAllByDeckIdOrderByCreatedAtAsc(Long deckId);

    List<VocabItem> findAllByDeckIdAndDeckOwnerIdOrderByCreatedAtAsc(Long deckId, Long ownerId);

    Optional<VocabItem> findByIdAndDeckOwnerId(Long id, Long ownerId);

    long countByDeckId(Long deckId);

    long countByDeckOwnerId(Long ownerId);

    List<VocabItem> findAllByDeckOwnerIdOrderByCreatedAtAsc(Long ownerId);
}
