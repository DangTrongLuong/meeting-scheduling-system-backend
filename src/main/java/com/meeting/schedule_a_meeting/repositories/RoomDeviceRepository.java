package com.meeting.schedule_a_meeting.repositories;

import com.meeting.schedule_a_meeting.entities.RoomDevice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoomDeviceRepository extends JpaRepository<RoomDevice, Long> {
    List<RoomDevice> findByMeetingRoomId(Long roomId);
}
