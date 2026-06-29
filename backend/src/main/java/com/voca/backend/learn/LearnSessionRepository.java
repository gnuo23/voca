package com.voca.backend.learn;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LearnSessionRepository extends JpaRepository<LearnSession, Long> {

    Optional<LearnSession> findByIdAndUserId(Long id, Long userId);

    List<LearnSession> findAllByUserIdAndStatusOrderByCreatedAtDesc(Long userId, LearnSessionStatus status);

    Optional<LearnSession> findFirstByUserIdAndDeckIdAndStatusOrderByCreatedAtDesc(
            Long userId, Long deckId, LearnSessionStatus status);
}
