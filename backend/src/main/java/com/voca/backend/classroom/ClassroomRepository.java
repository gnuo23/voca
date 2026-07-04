package com.voca.backend.classroom;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ClassroomRepository extends JpaRepository<Classroom, Long> {

    Optional<Classroom> findByInviteCode(String inviteCode);

    boolean existsByInviteCode(String inviteCode);
}
