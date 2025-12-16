package com.meeting.schedule_a_meeting.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.meeting.schedule_a_meeting.entities.UserStatus;

@Repository
public interface UserStatusRepository extends JpaRepository<UserStatus, UUID> {
    Optional<UserStatus> findByUserId(UUID userId);
}