package com.voca.backend.classroom;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

public interface ClassroomMemberRepository extends JpaRepository<ClassroomMember, Long> {

    @EntityGraph(attributePaths = {"classroom", "user"})
    List<ClassroomMember> findAllByUserIdOrderByJoinedAtDesc(Long userId);

    @EntityGraph(attributePaths = "user")
    List<ClassroomMember> findAllByClassroomIdOrderByJoinedAtAsc(Long classroomId);

    @EntityGraph(attributePaths = "user")
    List<ClassroomMember> findAllByClassroomIdInOrderByJoinedAtAsc(List<Long> classroomIds);

    Optional<ClassroomMember> findByClassroomIdAndUserId(Long classroomId, Long userId);

    boolean existsByClassroomIdAndUserId(Long classroomId, Long userId);
}
