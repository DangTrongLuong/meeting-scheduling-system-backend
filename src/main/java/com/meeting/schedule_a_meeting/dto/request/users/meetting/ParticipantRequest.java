package com.meeting.schedule_a_meeting.dto.request.users.meetting;

import com.meeting.schedule_a_meeting.enums.ParticipantRole;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    private ParticipantRole role = ParticipantRole.REQUIRED;
}
