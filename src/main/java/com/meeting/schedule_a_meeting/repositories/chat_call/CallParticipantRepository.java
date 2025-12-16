package com.meeting.schedule_a_meeting.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.meeting.schedule_a_meeting.entities.CallParticipant;

@Repository
public interface CallParticipantRepository extends JpaRepository<CallParticipant, UUID> {

    // Kiểm tra user có tham gia call không
    @Query("SELECT CASE WHEN COUNT(cp) > 0 THEN true ELSE false END " +
            "FROM CallParticipant cp WHERE cp.callSession.id = :callId AND cp.user.id = :userId")
    boolean existsByCallAndUser(@Param("callId") UUID callId, @Param("userId") UUID userId);

    // Lấy participant của user trong call
    Optional<CallParticipant> findByCallSessionIdAndUserId(UUID callSessionId, UUID userId);

    // Lấy tất cả participant của call
    List<CallParticipant> findByCallSessionId(UUID callSessionId);

    // Đếm số lượng người đã accepted
    @Query("SELECT COUNT(cp) FROM CallParticipant cp WHERE cp.callSession.id = :callId " +
            "AND cp.status IN ('ACCEPTED', 'LEFT')")
    long countAcceptedParticipants(@Param("callId") UUID callId);
}