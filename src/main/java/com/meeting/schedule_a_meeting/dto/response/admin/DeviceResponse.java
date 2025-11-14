package com.meeting.schedule_a_meeting.dto.response.admin;

import com.meeting.schedule_a_meeting.enums.DeviceStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DeviceResponse {
    private String id;
    private String name;
    private String imagePath;
    private DeviceStatus status;
    private int totalQuantity;
    private int availableQuantity;
}
