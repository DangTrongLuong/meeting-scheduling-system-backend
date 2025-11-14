package com.meeting.schedule_a_meeting.dto.request.admin;

import org.springframework.web.multipart.MultipartFile;

import com.meeting.schedule_a_meeting.enums.DeviceStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DeviceUpdateRequest {
    @NotBlank
    @Size(max = 100)
    private String name;

    @NotNull
    @PositiveOrZero
    private Integer quantity;

    @NotNull
    private DeviceStatus status;

    private MultipartFile image;
}
