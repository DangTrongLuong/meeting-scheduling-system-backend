package com.meeting.schedule_a_meeting.dto.request.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class AssignDeviceRequest {
    @NotBlank
    private String roomId;

    @NotBlank
    private String deviceId;

    @NotNull
    @Positive
    private Integer quantity;
}