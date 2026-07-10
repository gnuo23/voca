package com.voca.backend.toeic;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ToeicAttemptAnswerRepository extends JpaRepository<ToeicAttemptAnswer, Long> {

    Optional<ToeicAttemptAnswer> findByAttemptIdAndQuestionId(Long attemptId, Long questionId);

    List<ToeicAttemptAnswer> findAllByAttemptIdOrderByIdAsc(Long attemptId);

    long countByAttemptId(Long attemptId);

    @Query("select a.question.questionPart as part, count(a) as answered, "
            + "sum(case when a.correct = true then 1 else 0 end) as correct "
            + "from ToeicAttemptAnswer a "
            + "where a.attempt.user.id = :userId and a.attempt.status = :status "
            + "group by a.question.questionPart order by a.question.questionPart")
    List<PartStats> findPartStats(@Param("userId") Long userId, @Param("status") String status);

    interface PartStats {
        String getPart();
        long getAnswered();
        long getCorrect();
    }
}
