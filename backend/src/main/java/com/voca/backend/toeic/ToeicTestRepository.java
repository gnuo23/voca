package com.voca.backend.toeic;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ToeicTestRepository extends JpaRepository<ToeicTest, Long> {

    Optional<ToeicTest> findBySlug(String slug);

    boolean existsBySlug(String slug);

    List<ToeicTest> findAllByOrderByCollectionNameAscTestNumberAsc();
}
