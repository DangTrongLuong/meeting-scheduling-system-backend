package com.meeting.schedule_a_meeting.dto.request.users.meetting;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceRequest {

    @NotBlank(message = "Device ID is required")
    private String deviceId;

    @Min(value = 1, message = "Quantity must be at least 1")
    private int quantity;

    private String notes;
}