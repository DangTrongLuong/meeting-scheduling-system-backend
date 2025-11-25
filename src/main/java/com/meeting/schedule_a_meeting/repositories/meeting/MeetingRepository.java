package com.meeting.schedule_a_meeting.repositories.meeting;

import com.meeting.schedule_a_meeting.entities.Meeting;
import com.meeting.schedule_a_meeting.enums.MeetingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface MeetingRepository extends JpaRepository<Meeting, String> {

    // Kiểm tra xung đột thời gian cho cùng phòng
    @Query("SELECT m FROM Meeting m WHERE m.meetingRoom.id = :roomId " +
            "AND m.status != 'CANCELLED' " +
            "AND NOT (m.endTime <= :startTime OR m.startTime >= :endTime)")
    List<Meeting> findConflictingMeetings(
            @Param("roomId") String roomId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );


    @Query("SELECT m FROM Meeting m WHERE m.meetingRoom.id = :roomId " +
            "AND m.id != :meetingId " +
            "AND m.status != 'CANCELLED' " +
            "AND NOT (m.endTime <= :startTime OR m.startTime >= :endTime)")
    List<Meeting> findConflictingMeetingsExcludingCurrent(
            @Param("roomId") String roomId,
            @Param("meetingId") String meetingId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );


    @Query("SELECT DISTINCT m FROM Meeting m " +
            "LEFT JOIN m.participants p " +
            "WHERE (m.creator.id = :userId OR p.user.id = :userId) " +
            "AND m.status != 'CANCELLED' " +
            "ORDER BY m.startTime DESC")
    List<Meeting> findMeetingsByUser(@Param("userId") UUID userId);


    @Query("SELECT m FROM Meeting m WHERE m.meetingRoom.id = :roomId " +
            "AND m.status != 'CANCELLED' " +
            "AND m.startTime >= :startDate " +
            "AND m.endTime <= :endDate " +
            "ORDER BY m.startTime ASC")
    List<Meeting> findMeetingsByRoomAndDateRange(
            @Param("roomId") String roomId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );


    List<Meeting> findByCreatorIdOrderByStartTimeDesc(UUID creatorId);


    List<Meeting> findByStatusOrderByStartTimeAsc(MeetingStatus status);

    @Query("SELECT m FROM Meeting m WHERE m.status = 'SCHEDULED' " +
            "AND m.startTime BETWEEN :now AND :reminderTime")
    List<Meeting> findUpcomingMeetingsForReminder(
            @Param("now") LocalDateTime now,
            @Param("reminderTime") LocalDateTime reminderTime
    );


    @Query("""
        SELECT m FROM Meeting m
        JOIN FETCH m.creator
        WHERE m.status = 'SCHEDULED'
        AND m.startTime BETWEEN :now AND :reminderTime
        """)
    List<Meeting> findMeetingsForCreatorReminder(@Param("now") LocalDateTime now,
                                                 @Param("reminderTime") LocalDateTime reminderTime);

}
