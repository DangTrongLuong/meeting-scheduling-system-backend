package com.meeting.schedule_a_meeting.repositories;

import com.meeting.schedule_a_meeting.entities.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MeetingRepository extends JpaRepository<Meeting, Long> {
    List<Meeting> findByRoomAndStartTimeLessThanEqualAndEndTimeGreaterThanEqual(
            String room, LocalDateTime endTime, LocalDateTime startTime);
}
