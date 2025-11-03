package com.meeting.schedule_a_meeting.repositories;

import com.meeting.schedule_a_meeting.entities.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<Users, UUID> {
    boolean existsByEmail(String email);
    Optional<Users> findByEmail(String email);
    Optional<Users> findById(String id);
}
