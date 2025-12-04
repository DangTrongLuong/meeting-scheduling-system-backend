package com.meeting.schedule_a_meeting.dto.response.users;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoogleCalendarSyncResponse {
    private int syncedCount;
    private int failedCount;
    private String message;
}
