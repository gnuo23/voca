package com.voca.backend.deck;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeckRepository extends JpaRepository<Deck, Long> {

    List<Deck> findAllByOwnerIdOrderByUpdatedAtDesc(Long ownerId);

    Optional<Deck> findByIdAndOwnerId(Long id, Long ownerId);
}
