package com.meeting.schedule_a_meeting.dto.response.users.call;

import java.time.LocalDateTime;
import java.util.UUID;

import com.meeting.schedule_a_meeting.enums.CallParticipantStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CallParticipantResponse {

    private UUID participantId;
    private UUID userId;
    private String userName;
    private String userAvatar;
    private CallParticipantStatus status;
    private LocalDateTime joinedAt;
    private LocalDateTime leftAt;
    private Long durationSeconds;
    private boolean isMuted;
    private boolean isVideoEnabled;
}
