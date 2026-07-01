package com.voca.backend.deck;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DeckShareCodeRepository extends JpaRepository<DeckShareCode, Long> {

    Optional<DeckShareCode> findByDeckId(Long deckId);

    Optional<DeckShareCode> findByCode(String code);

    boolean existsByCode(String code);
}
