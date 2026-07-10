package com.voca.backend.toeic;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ToeicQuestionRepository extends JpaRepository<ToeicQuestion, Long> {

    List<ToeicQuestion> findAllByTestIdOrderByQuestionNumberAsc(Long testId);

    List<ToeicQuestion> findAllByTestIdAndQuestionPartOrderByQuestionNumberAsc(Long testId, String questionPart);

    @Query("select q.questionPart as part, count(q) as total from ToeicQuestion q where q.test.id = :testId group by q.questionPart")
    List<PartCount> countByPart(@Param("testId") Long testId);

    interface PartCount {
        String getPart();

        long getTotal();
    }
}
