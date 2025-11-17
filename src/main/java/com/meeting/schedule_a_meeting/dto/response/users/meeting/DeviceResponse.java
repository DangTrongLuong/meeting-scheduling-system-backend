package com.meeting.schedule_a_meeting.dto.response.users.meeting;

import com.meeting.schedule_a_meeting.enums.MeetingDeviceStatus;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceResponse {
    private String id;
    private DeviceSummary device;
    private int quantity;
    private MeetingDeviceStatus status;
    private String notes;
}
