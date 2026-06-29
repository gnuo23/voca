package com.voca.backend.learn;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LearnAnswerRepository extends JpaRepository<LearnAnswer, Long> {

    List<LearnAnswer> findAllBySessionIdOrderByAnsweredAtAsc(Long sessionId);
}
