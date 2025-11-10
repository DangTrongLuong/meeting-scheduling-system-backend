package com.meeting.schedule_a_meeting.dto.request.admin;

import lombok.Data;

@Data
public class AssignDeviceRequest {
    private Long roomId;
    private Long deviceId;
    private int quantity;
}
