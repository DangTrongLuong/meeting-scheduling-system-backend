package com.meeting.schedule_a_meeting.dto.request.admin;

import com.meeting.schedule_a_meeting.enums.RoomDeviceStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequest(
        @NotNull(message = "Status is required") RoomDeviceStatus status) {
}
