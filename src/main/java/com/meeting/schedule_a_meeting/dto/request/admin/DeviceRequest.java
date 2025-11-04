package com.meeting.schedule_a_meeting.dto.request.admin;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeviceRequest {
    private String name;
    private String description;
    private boolean active;
}

