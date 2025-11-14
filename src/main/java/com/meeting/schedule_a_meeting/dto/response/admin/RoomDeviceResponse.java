package com.meeting.schedule_a_meeting.dto.response.admin;

import com.meeting.schedule_a_meeting.enums.RoomDeviceStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RoomDeviceResponse {
    private String id;
    private String meetingRoom;
    private String roomName;
    private String device;
    private String deviceName;
    private int quantity;
    private RoomDeviceStatus status;
}