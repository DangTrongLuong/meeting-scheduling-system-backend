package com.meeting.schedule_a_meeting.dto.response.admin;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RoomDeviceResponse {
    private Long id;
    private String roomName;
    private String deviceName;
    private int quantity;
    private String status;
}
