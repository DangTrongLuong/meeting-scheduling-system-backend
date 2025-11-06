package com.meeting.schedule_a_meeting.mapper.admin;

import com.meeting.schedule_a_meeting.dto.request.admin.DeviceRequest;
import com.meeting.schedule_a_meeting.dto.response.admin.DeviceResponse;
import com.meeting.schedule_a_meeting.entities.Device;
import org.springframework.stereotype.Component;

@Component
public class DeviceMapper {

    public DeviceResponse toResponse(Device device) {
        return DeviceResponse.builder()
                .id(device.getId())              // Long id
                .name(device.getName())
                .active(device.isActive())
                .quantity(device.getQuantity())
                .build();
    }

    public Device toEntity(DeviceRequest request) {
        return Device.builder()
                // id để DB tự sinh, không set ở đây
                .name(request.getName())
                .active(request.isActive())
                .quantity(request.getQuantity())
                .build();
    }
}
