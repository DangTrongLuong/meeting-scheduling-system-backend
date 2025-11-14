package com.meeting.schedule_a_meeting.dto.request.admin;

import org.springframework.web.multipart.MultipartFile;

import com.meeting.schedule_a_meeting.entities.Device;
import com.meeting.schedule_a_meeting.enums.DeviceStatus;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
public class DeviceCreateRequest {
    @NotBlank
    @Size(max = 100)
    private String name;

    @NotNull
    @PositiveOrZero
    private Integer quantity;

    @NotNull
    private DeviceStatus status;

    @NotNull
    private MultipartFile image;
}
