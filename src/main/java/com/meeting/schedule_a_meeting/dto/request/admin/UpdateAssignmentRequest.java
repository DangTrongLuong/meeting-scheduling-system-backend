package com.meeting.schedule_a_meeting.dto.request.admin;

import com.meeting.schedule_a_meeting.enums.RoomDeviceStatus;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateAssignmentRequest {

    @NotBlank
    private String roomId;

    @NotBlank
    private String deviceId;

    @Min(1)
    private Integer quantity;

    @NotNull
    private RoomDeviceStatus status;
}