package com.meeting.schedule_a_meeting.mapper.admin;


import com.meeting.schedule_a_meeting.dto.request.admin.DeviceRequest;
import com.meeting.schedule_a_meeting.dto.response.admin.DeviceResponse;
import com.meeting.schedule_a_meeting.entities.Device;
import org.springframework.stereotype.Component;

@Component
public class DeviceMapper {
    public DeviceResponse toResponse(Device device) {
        return DeviceResponse.builder()
                .id(device.getId())
                .name(device.getName())
                .description(device.getDescription())
                .active(device.isActive())
                .build();
    }

    public Device toEntity(DeviceRequest request) {
        return Device.builder()
                .name(request.getName())
                .description(request.getDescription())
                .active(request.isActive())
                .build();
    }
}

