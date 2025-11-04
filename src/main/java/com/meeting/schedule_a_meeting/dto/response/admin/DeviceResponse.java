package com.meeting.schedule_a_meeting.dto.response.admin;


import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DeviceResponse {
    UUID id;
    String name;
    String description;
    boolean active;
}

