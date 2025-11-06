package com.meeting.schedule_a_meeting.repositories;

import com.meeting.schedule_a_meeting.entities.Users;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AdminRepository extends JpaRepository<Users, UUID> {
    Optional<Users> findByEmail(String email);
}
