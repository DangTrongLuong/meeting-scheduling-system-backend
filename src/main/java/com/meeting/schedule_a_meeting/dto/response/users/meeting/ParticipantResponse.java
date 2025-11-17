package com.meeting.schedule_a_meeting.dto.response.users.meeting;

import com.meeting.schedule_a_meeting.entities.Users;
import com.meeting.schedule_a_meeting.enums.ParticipantRole;
import com.meeting.schedule_a_meeting.enums.ParticipantStatus;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantResponse {
    private String id;
    private UserSummary user;
    private ParticipantRole role;
    private ParticipantStatus status;
    private LocalDateTime invitedAt;
    private LocalDateTime respondedAt;
}
