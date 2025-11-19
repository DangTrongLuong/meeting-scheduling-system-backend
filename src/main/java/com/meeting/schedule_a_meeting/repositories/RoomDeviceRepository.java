package com.meeting.schedule_a_meeting.repositories;

import com.meeting.schedule_a_meeting.entities.RoomDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RoomDeviceRepository extends JpaRepository<RoomDevice, String> {
    List<RoomDevice> findByMeetingRoomId(String roomId);
    List<RoomDevice> findByDeviceId(String deviceId);

    List<RoomDevice> findByMeetingRoomIdAndDeviceId(String roomId, String deviceId);

    boolean existsByMeetingRoomIdAndDeviceId(String roomId, String deviceId);

    @Query("SELECT rd FROM RoomDevice rd WHERE rd.meetingRoom.id = :roomId AND rd.device.status = 'ACTIVE'")
    List<RoomDevice> findActiveDevicesByRoom(@Param("roomId") String roomId);
}
