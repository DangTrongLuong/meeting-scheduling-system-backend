package com.meeting.schedule_a_meeting.dto.request.users.call;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InitiateCallRequest {

    @NotBlank(message = "Meeting ID is required")
    private String meetingId;
}
