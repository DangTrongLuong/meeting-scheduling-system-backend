package com.meeting.schedule_a_meeting.dto.response.admin;


import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceResponse {
    private Long id;
    private String name;
    private boolean active;
    private int quantity;
}

