package com.meeting.schedule_a_meeting.dto.request.users.meetting;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateMeetingRequest {

    @NotBlank(message = "Title is required")
    @Size(min = 5, max = 200, message = "Title must be between 5 and 200 characters")
    private String title;

    private String description;
    private String date;

    @NotNull(message = "Start time is required")
    private String startTime;

    @NotNull(message = "End time is required")
    private String endTime;

    @NotBlank(message = "Room ID is required")
    private String roomId;

    @JsonProperty("isRepeat")
    private boolean isRepeat;

    private List<String> repeatDays;

    @JsonProperty("repeatType") // "DAILY", "WEEKLY", "CUSTOM" hoặc null
    private String repeatType;

    @JsonProperty("repeatWeeks") // số tuần lặp (1-36), chỉ dùng khi WEEKLY
    private Integer repeatWeeks;

    @JsonProperty("repeatEndAfterMonths") // 1 hoặc 2, chỉ dùng khi DAILY hoặc CUSTOM
    private Integer repeatEndAfterMonths;

    private List<ParticipantRequest> participants;

    // private List<DeviceRequest> devices;
    private List<DeviceBorrowRequest> borrowedDevices;
}