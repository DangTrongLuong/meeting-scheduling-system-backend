package com.meeting.schedule_a_meeting.dto.response.users.call;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TodayMeetingsResponse {

    private String meetingId;
    private String title;
    private String description;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String roomName;
    private long participantCount;
    private boolean isCallActive;
    private boolean canInitiateCall;
}