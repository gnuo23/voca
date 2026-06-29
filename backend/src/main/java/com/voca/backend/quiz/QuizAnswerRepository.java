package com.voca.backend.quiz;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuizAnswerRepository extends JpaRepository<QuizAnswer, Long> {

    List<QuizAnswer> findAllByAttemptIdOrderByIdAsc(Long attemptId);

    Optional<QuizAnswer> findByAttemptIdAndQuestionId(Long attemptId, Long questionId);

    long countByAttemptId(Long attemptId);

    int countByAttemptIdAndCorrect(Long attemptId, boolean correct);
}
