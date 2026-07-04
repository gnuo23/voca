package com.voca.backend.classroom;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ClassroomMemberRepository extends JpaRepository<ClassroomMember, Long> {

    List<ClassroomMember> findAllByUserIdOrderByJoinedAtDesc(Long userId);

    List<ClassroomMember> findAllByClassroomIdOrderByJoinedAtAsc(Long classroomId);

    Optional<ClassroomMember> findByClassroomIdAndUserId(Long classroomId, Long userId);

    boolean existsByClassroomIdAndUserId(Long classroomId, Long userId);
}
