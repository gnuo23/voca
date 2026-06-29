package com.voca.backend.quiz;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findAllByIdInAndOwnerId(Collection<Long> ids, Long ownerId);

    Optional<Question> findByIdAndOwnerId(Long id, Long ownerId);
}
