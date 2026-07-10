package com.voca.backend.toeic;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ToeicQuestionGroupRepository extends JpaRepository<ToeicQuestionGroup, Long> {

    List<ToeicQuestionGroup> findAllByTestIdOrderByGroupOrderAsc(Long testId);
}
