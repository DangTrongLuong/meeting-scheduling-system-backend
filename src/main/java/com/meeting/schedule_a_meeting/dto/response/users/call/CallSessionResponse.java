package com.meeting.schedule_a_meeting.dto.response.users.call;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.meeting.schedule_a_meeting.enums.CallStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CallSessionResponse {

    private UUID callId;
    private String meetingId;
    private String meetingTitle;
    private UUID initiatorId;
    private String initiatorName;
    private CallStatus status;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private Long durationSeconds;
    private List<CallParticipantResponse> participants;
}
