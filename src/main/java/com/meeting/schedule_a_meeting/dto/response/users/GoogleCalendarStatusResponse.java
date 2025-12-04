package com.meeting.schedule_a_meeting.dto.response.users;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoogleCalendarStatusResponse {
    private boolean connected;
    private String googleEmail;
    private LocalDateTime connectedAt;
    private LocalDateTime lastSyncedAt;
}
