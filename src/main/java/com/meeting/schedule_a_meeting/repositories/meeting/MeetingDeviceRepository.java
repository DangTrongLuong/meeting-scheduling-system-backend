package com.meeting.schedule_a_meeting.repositories.meeting;

import com.meeting.schedule_a_meeting.entities.MeetingDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MeetingDeviceRepository extends JpaRepository<MeetingDevice, String> {

    Optional<MeetingDevice> findByMeetingIdAndDeviceId(String meetingId, String deviceId);

    boolean existsByMeetingIdAndDeviceId(String meetingId, String deviceId);

    List<MeetingDevice> findByMeetingId(String meetingId);

    // Tính tổng số lượng device đã đặt trong khoảng thời gian (để check availability)
    @Query("SELECT COALESCE(SUM(md.quantity), 0) FROM MeetingDevice md " +
            "JOIN md.meeting m " +
            "WHERE md.device.id = :deviceId " +
            "AND m.status != 'CANCELLED' " +
            "AND NOT (m.endTime <= :startTime OR m.startTime >= :endTime)")
    int getTotalReservedQuantity(
            @Param("deviceId") String deviceId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    // Tính tổng số lượng device đã đặt, loại trừ meeting hiện tại
    @Query("SELECT COALESCE(SUM(md.quantity), 0) FROM MeetingDevice md " +
            "JOIN md.meeting m " +
            "WHERE md.device.id = :deviceId " +
            "AND m.id != :meetingId " +
            "AND m.status != 'CANCELLED' " +
            "AND NOT (m.endTime <= :startTime OR m.startTime >= :endTime)")
    int getTotalReservedQuantityExcludingMeeting(
            @Param("deviceId") String deviceId,
            @Param("meetingId") String meetingId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    // Xóa tất cả devices của meeting
    void deleteByMeetingId(String meetingId);
}