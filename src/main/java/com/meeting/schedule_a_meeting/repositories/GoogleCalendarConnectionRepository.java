package com.meeting.schedule_a_meeting.repositories;

import com.meeting.schedule_a_meeting.entities.GoogleCalendarConnection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface GoogleCalendarConnectionRepository extends JpaRepository<GoogleCalendarConnection, UUID> {

    Optional<GoogleCalendarConnection> findByUserIdAndIsActiveTrue(UUID userId);

    boolean existsByUserIdAndIsActiveTrue(UUID userId);

    void deleteByUserId(UUID userId);
}