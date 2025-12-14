package com.meeting.schedule_a_meeting.dto.response.users.meeting;

import java.time.LocalDateTime;
import java.util.List;

import com.meeting.schedule_a_meeting.enums.MeetingStatus;

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
    private String id;
    private String title;
    private String description;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private MeetingStatus status;
    private boolean isRepeat;
    private RoomSummary room;
    private UserSummary creator;
    private List<ParticipantResponse> participants;
    private List<DeviceResponse> devices;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean hasConcluded;
}