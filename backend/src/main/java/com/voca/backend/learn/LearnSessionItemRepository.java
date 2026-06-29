package com.voca.backend.learn;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LearnSessionItemRepository extends JpaRepository<LearnSessionItem, Long> {

    List<LearnSessionItem> findAllBySessionIdOrderByPriorityDescLastAnsweredAtAsc(Long sessionId);

    List<LearnSessionItem> findAllBySessionId(Long sessionId);

    long countBySessionIdAndStage(Long sessionId, LearnItemStage stage);
}
