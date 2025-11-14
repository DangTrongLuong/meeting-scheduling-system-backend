package com.meeting.schedule_a_meeting.repositories;

import com.meeting.schedule_a_meeting.entities.RoomDevice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoomDeviceRepository extends JpaRepository<RoomDevice, String> {
    List<RoomDevice> findByMeetingRoomId(String roomId);

    List<RoomDevice> findByMeetingRoomIdAndDeviceId(String roomId, String deviceId);
}
