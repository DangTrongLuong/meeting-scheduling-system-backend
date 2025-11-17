package com.meeting.schedule_a_meeting.dto.response.users.meeting;

import com.meeting.schedule_a_meeting.entities.MeetingRoom;
import com.meeting.schedule_a_meeting.entities.Users;
import com.meeting.schedule_a_meeting.enums.MeetingStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeetingResponse {
    private String id;
    private String title;
    private String description;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private MeetingStatus status;
    private RoomSummary room;
    private UserSummary creator;
    private List<ParticipantResponse> participants;
    private List<DeviceResponse> devices;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}