package com.meeting.schedule_a_meeting.mapper.admin;

import com.meeting.schedule_a_meeting.dto.request.admin.DeviceCreateRequest;
import com.meeting.schedule_a_meeting.dto.request.admin.DeviceUpdateRequest;
import com.meeting.schedule_a_meeting.dto.response.admin.DeviceResponse;
import com.meeting.schedule_a_meeting.entities.Device;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface DeviceMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "imagePath", ignore = true)
    @Mapping(target = "totalQuantity", source = "quantity")
    @Mapping(target = "availableQuantity", source = "quantity")
    Device toEntity(DeviceCreateRequest request);

    DeviceResponse toResponse(Device entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "imagePath", ignore = true)
    @Mapping(target = "totalQuantity", ignore = true)
    @Mapping(target = "availableQuantity", ignore = true)
    void updateEntity(@MappingTarget Device entity, DeviceUpdateRequest request);
}
