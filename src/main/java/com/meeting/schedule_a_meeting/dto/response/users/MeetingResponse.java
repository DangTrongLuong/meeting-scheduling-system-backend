package com.meeting.schedule_a_meeting.dto.response.users;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeetingResponse {
    private Long id;
    private String title;
    private String room;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String roomName;
    private String createdBy;
    private String status;
    private boolean isRepeat;
    private List<String> invitedEmails;

}