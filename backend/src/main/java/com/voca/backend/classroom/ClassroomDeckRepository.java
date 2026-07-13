package com.voca.backend.classroom;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClassroomDeckRepository extends JpaRepository<ClassroomDeck, Long> {

    @EntityGraph(attributePaths = "deck")
    List<ClassroomDeck> findAllByClassroomIdOrderByAddedAtDesc(Long classroomId);

    @EntityGraph(attributePaths = "deck")
    List<ClassroomDeck> findAllByClassroomIdInOrderByAddedAtDesc(List<Long> classroomIds);

    @EntityGraph(attributePaths = "deck")
    Optional<ClassroomDeck> findByClassroomIdAndDeckId(Long classroomId, Long deckId);

    @Query("""
            select count(cd) > 0
            from ClassroomDeck cd
            join ClassroomMember cm on cm.classroom = cd.classroom
            where cd.deck.id = :deckId and cm.user.id = :userId
            """)
    boolean existsStudyAccess(@Param("deckId") Long deckId, @Param("userId") Long userId);

    @Query("""
            select distinct cd.deck.id
            from ClassroomDeck cd
            join ClassroomMember cm on cm.classroom = cd.classroom
            where cm.user.id = :userId
            """)
    List<Long> findStudyDeckIds(@Param("userId") Long userId);

    boolean existsByClassroomIdAndDeckId(Long classroomId, Long deckId);

    long countByClassroomId(Long classroomId);
}
