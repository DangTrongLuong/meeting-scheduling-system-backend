package com.meeting.schedule_a_meeting.repositories;

import com.meeting.schedule_a_meeting.entities.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MeetingRepository extends JpaRepository<Meeting, Long> {

    // Check for conflicts when creating a meeting
    List<Meeting> findByRoom_IdAndStartTimeLessThanEqualAndEndTimeGreaterThanEqual(
            Long roomId,
            LocalDateTime endTime,
            LocalDateTime startTime
    );

    // Get schedule for a room within a date range
    List<Meeting> findByRoom_IdAndStartTimeBetweenOrderByStartTimeAsc(
            Long roomId,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime
    );
}