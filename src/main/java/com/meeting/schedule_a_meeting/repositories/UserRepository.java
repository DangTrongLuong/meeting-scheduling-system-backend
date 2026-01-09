package com.meeting.schedule_a_meeting.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.meeting.schedule_a_meeting.entities.Users;

public interface UserRepository extends JpaRepository<Users, UUID> {
    boolean existsByEmail(String email);

    Optional<Users> findByEmail(String email);

    Optional<Users> findById(UUID id);

    Optional<Users> findByAccessToken(String accessToken);

    Optional<Users> findByEmailIgnoreCase(String email);

    @Query("SELECT u.email FROM Users u WHERE u.email LIKE %:query%")
    List<String> searchEmailByQuery(String query);

    List<Users> findTop10ByEmailContainingIgnoreCase(String emailPart);

    Page<Users> findAll(Pageable pageable);

    @Query("SELECT u FROM Users u WHERE " +
            "(LOWER(u.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND u.id != :currentUserId " +
            "ORDER BY u.name ASC")
    List<Users> findByNameContainingIgnoreCaseOrEmailContainingIgnoreCaseAndIdNot(
            @Param("keyword") String keyword1,
            @Param("keyword") String keyword2,
            @Param("currentUserId") UUID currentUserId);
}
