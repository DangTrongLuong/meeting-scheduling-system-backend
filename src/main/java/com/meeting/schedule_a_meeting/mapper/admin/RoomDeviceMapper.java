package com.meeting.schedule_a_meeting.mapper.admin;

import com.meeting.schedule_a_meeting.dto.response.admin.RoomDeviceResponse;
import com.meeting.schedule_a_meeting.entities.Device;
import com.meeting.schedule_a_meeting.entities.MeetingRoom;
import com.meeting.schedule_a_meeting.entities.RoomDevice;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface RoomDeviceMapper {

    @Mapping(target = "roomName", source = "meetingRoom.name")
    @Mapping(target = "deviceName", source = "device.name")
    @Mapping(target = "meetingRoom", source = "meetingRoom", qualifiedByName = "mapMeetingRoomId")
    @Mapping(target = "device", source = "device", qualifiedByName = "mapDeviceId")
    RoomDeviceResponse toResponse(RoomDevice entity);

    @Named("mapMeetingRoomId")
    default String mapMeetingRoomId(MeetingRoom room) {
        return room != null ? room.getId() : null;
    }

    @Named("mapDeviceId")
    default String mapDeviceId(Device device) {
        return device != null ? device.getId() : null;
    }
}