package com.meeting.schedule_a_meeting.dto.request.users.meetting;

import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMeetingRequest {

    @Size(min = 5, max = 200, message = "Title must be between 5 and 200 characters")
    private String title;

    private String description;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String roomId;

    private List<ParticipantRequest> participants;

    private List<DeviceRequest> devices;
}
