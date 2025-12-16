package com.meeting.schedule_a_meeting.dto.request.users.call;

import java.util.UUID;

import com.meeting.schedule_a_meeting.enums.CallAction;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CallActionRequest {

    @NotNull(message = "Call ID is required")
    private UUID callId;

    @NotNull(message = "Action is required")
    private CallAction action;

}
