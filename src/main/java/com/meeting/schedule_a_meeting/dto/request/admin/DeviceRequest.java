package com.meeting.schedule_a_meeting.dto.request.admin;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceRequest {
    @NotBlank
    private String name;
    private boolean active;
    @Min(0)
    private int quantity;
}

