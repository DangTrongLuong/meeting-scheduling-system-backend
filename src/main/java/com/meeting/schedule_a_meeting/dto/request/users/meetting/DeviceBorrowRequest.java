package com.meeting.schedule_a_meeting.dto.request.users.meetting;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceBorrowRequest {
    @NotBlank
    private String deviceId;
    @Min(1)
    private int quantity;
    private String notes;
}