package com.meeting.schedule_a_meeting.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.meeting.schedule_a_meeting.entities.Users;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<Users, UUID> {
    boolean existsByEmail(String email);

    Optional<Users> findByEmail(String email);

    Optional<Users> findById(UUID id);

    Optional<Users> findByAccessToken(String accessToken);

    @Query("SELECT u.email FROM Users u WHERE u.email LIKE %:query%")
    List<String> searchEmailByQuery(String query);

    List<Users> findTop10ByEmailContainingIgnoreCase(String emailPart);
}
