package com.meeting.schedule_a_meeting.repositories.chat_call;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.meeting.schedule_a_meeting.entities.CallSession;

@Repository
public interface CallSessionRepository extends JpaRepository<CallSession, UUID> {

    // Lấy call đang active của một meeting
    @Query("SELECT cs FROM CallSession cs WHERE cs.meeting.id = :meetingId AND " +
            "(cs.status = 'INITIATED' OR cs.status = 'RINGING' OR cs.status = 'ANSWERED') " +
            "ORDER BY cs.startedAt DESC")
    Optional<CallSession> findActiveCallByMeeting(@Param("meetingId") String meetingId);

    // Lấy tất cả call của một meeting
    @Query("SELECT cs FROM CallSession cs WHERE cs.meeting.id = :meetingId ORDER BY cs.startedAt DESC")
    List<CallSession> findByMeetingId(@Param("meetingId") String meetingId);

    // Lấy call được khởi tạo bởi user trong khoảng thời gian
    @Query("SELECT cs FROM CallSession cs WHERE cs.initiator.id = :userId AND " +
            "cs.startedAt BETWEEN :startTime AND :endTime")
    List<CallSession> findByInitiatorAndTimeRange(
            @Param("userId") UUID userId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );
}
