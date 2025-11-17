package com.meeting.schedule_a_meeting.dto.request.users.meetting;

import com.meeting.schedule_a_meeting.enums.ParticipantRole;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantRequest {

    @NotNull(message = "User ID is required")
    private UUID userId;

    private ParticipantRole role = ParticipantRole.REQUIRED;
}
