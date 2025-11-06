package com.meeting.schedule_a_meeting.repositories;

import com.meeting.schedule_a_meeting.entities.MeetingRoom;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeetingRoomRepository extends JpaRepository<MeetingRoom, Long> {
}