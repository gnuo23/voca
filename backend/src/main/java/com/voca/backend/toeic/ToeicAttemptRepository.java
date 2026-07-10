package com.voca.backend.toeic;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ToeicAttemptRepository extends JpaRepository<ToeicAttempt, Long> {

    Optional<ToeicAttempt> findByIdAndUserId(Long id, Long userId);

    List<ToeicAttempt> findAllByUserIdOrderByStartedAtDesc(Long userId);

    List<ToeicAttempt> findAllByUserIdAndStatusOrderByCompletedAtDesc(Long userId, String status);
}
