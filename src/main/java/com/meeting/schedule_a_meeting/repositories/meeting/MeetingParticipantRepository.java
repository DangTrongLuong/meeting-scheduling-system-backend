package com.meeting.schedule_a_meeting.repositories.meeting;

import com.meeting.schedule_a_meeting.entities.MeetingParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MeetingParticipantRepository extends JpaRepository<MeetingParticipant, String> {

    // Tìm participant theo meeting và user
    Optional<MeetingParticipant> findByMeetingIdAndUserId(String meetingId, UUID userId);

    boolean existsByMeetingIdAndUserId(String meetingId, UUID userId);


    List<MeetingParticipant> findByMeetingId(String meetingId);

    // Tìm tất cả meetings mà user tham gia
    @Query("SELECT mp FROM MeetingParticipant mp " +
            "JOIN FETCH mp.meeting m " +
            "WHERE mp.user.id = :userId " +
            "AND m.status != 'CANCELLED' " +
            "ORDER BY m.startTime DESC")
    List<MeetingParticipant> findByUserIdWithMeeting(@Param("userId") UUID userId);


    void deleteByMeetingId(String meetingId);
}
