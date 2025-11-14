package com.meeting.schedule_a_meeting.mapper.admin;

import com.meeting.schedule_a_meeting.dto.request.admin.MeetingRoomRequest;
import com.meeting.schedule_a_meeting.dto.response.admin.MeetingRoomResponse;
import com.meeting.schedule_a_meeting.entities.MeetingRoom;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface MeetingRoomMapper {

    @Mapping(target = "id", ignore = true)
    MeetingRoom toEntity(MeetingRoomRequest request);

    MeetingRoomResponse toResponse(MeetingRoom entity);

    @Mapping(target = "id", ignore = true)
    void updateEntity(@MappingTarget MeetingRoom entity, MeetingRoomRequest request);
}
