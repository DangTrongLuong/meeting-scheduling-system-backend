package com.meeting.schedule_a_meeting.dto.response.users.meeting;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceSummary {
    private String id;
    private String name;
    private String imagePath;
}
