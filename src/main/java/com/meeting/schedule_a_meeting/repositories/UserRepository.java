package com.meeting.schedule_a_meeting.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.meeting.schedule_a_meeting.entities.Users;

public interface UserRepository extends JpaRepository<Users, UUID> {
    boolean existsByEmail(String email);

    Optional<Users> findByEmail(String email);

    Optional<Users> findById(UUID id);

    Optional<Users> findByAccessToken(String accessToken);
}
