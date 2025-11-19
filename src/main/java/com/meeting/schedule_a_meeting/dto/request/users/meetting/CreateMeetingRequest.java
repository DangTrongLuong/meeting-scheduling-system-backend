package com.meeting.schedule_a_meeting.dto.request.users.meetting;

import jakarta.validation.constraints.*;
import lombok.*;
import com.meeting.schedule_a_meeting.dto.request.users.meetting.DeviceBorrowRequest;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateMeetingRequest {

    @NotBlank(message = "Title is required")
    @Size(min = 5, max = 200, message = "Title must be between 5 and 200 characters")
    private String title;

    private String description;

    @NotNull(message = "Start time is required")
    private LocalDateTime startTime;

    @NotNull(message = "End time is required")
    private LocalDateTime endTime;

    @NotBlank(message = "Room ID is required")
    private String roomId;

    private List<ParticipantRequest> participants;

    // private List<DeviceRequest> devices;
    private List<DeviceBorrowRequest> borrowedDevices;
}