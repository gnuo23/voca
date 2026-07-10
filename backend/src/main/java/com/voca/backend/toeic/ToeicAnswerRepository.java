package com.voca.backend.toeic;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ToeicAnswerRepository extends JpaRepository<ToeicAnswer, Long> {

    List<ToeicAnswer> findAllByQuestionIdInOrderByAnswerOrderAsc(List<Long> questionIds);
}
