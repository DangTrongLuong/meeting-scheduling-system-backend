package com.meeting.schedule_a_meeting.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class UserStatusDto {
    private UUID userId;
    private String status;
}
