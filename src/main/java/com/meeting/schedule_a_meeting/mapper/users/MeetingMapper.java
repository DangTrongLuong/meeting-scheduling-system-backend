package com.meeting.schedule_a_meeting.mapper.users;


import com.meeting.schedule_a_meeting.dto.response.users.meeting.*;
import com.meeting.schedule_a_meeting.entities.*;
import org.mapstruct.Mapper;

import java.util.Collections;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public class MeetingMapper {

    public MeetingResponse toMeetingResponse(Meeting meeting) {
        if (meeting == null) return null;

        // Lớp an toàn: loại creator khỏi response participants
        var participants = meeting.getParticipants() != null
                ? meeting.getParticipants().stream()
                .filter(mp -> mp.getUser() != null
                        && !mp.getUser().getId().equals(meeting.getCreator().getId())) // ⛔ exclude creator
                .map(this::toParticipantResponse)
                .collect(Collectors.toList())
                : Collections.<ParticipantResponse>emptyList();

        var devices = meeting.getDevices() != null
                ? meeting.getDevices().stream()
                .map(this::toDeviceResponse)
                .collect(Collectors.toList())
                : Collections.<DeviceResponse>emptyList();

        return MeetingResponse.builder()
                .id(meeting.getId())
                .title(meeting.getTitle())
                .description(meeting.getDescription())
                .startTime(meeting.getStartTime())
                .endTime(meeting.getEndTime())
                .status(meeting.getStatus())
                .room(toRoomSummary(meeting.getMeetingRoom()))
                .creator(toUserSummary(meeting.getCreator()))
                .participants(participants)  // <-- đã loại creator
                .devices(devices)
                .createdAt(meeting.getCreatedAt())
                .updatedAt(meeting.getUpdatedAt())
                .build();
    }

    public ParticipantResponse toParticipantResponse(MeetingParticipant participant) {
        if (participant == null) {
            return null;
        }

        return ParticipantResponse.builder()
                .id(participant.getId())
                .user(toUserSummary(participant.getUser()))
                .role(participant.getRole())
                .status(participant.getStatus())
                .invitedAt(participant.getInvitedAt())
                .respondedAt(participant.getRespondedAt())
                .build();
    }

    public DeviceResponse toDeviceResponse(MeetingDevice meetingDevice) {
        if (meetingDevice == null) {
            return null;
        }

        return DeviceResponse.builder()
                .id(meetingDevice.getId())
                .device(toDeviceSummary(meetingDevice.getDevice()))
                .quantity(meetingDevice.getQuantity())
                .status(meetingDevice.getStatus())
                .notes(meetingDevice.getNotes())
                .build();
    }

    public UserSummary toUserSummary(Users user) {
        if (user == null) {
            return null;
        }

        return UserSummary.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .avatarUrl(user.getAvatar_url())
                .build();
    }

    public RoomSummary toRoomSummary(MeetingRoom room) {
        if (room == null) {
            return null;
        }

        return RoomSummary.builder()
                .id(room.getId())
                .name(room.getName())
                .location(room.getLocation())
                .capacity(room.getCapacity())
                .build();
    }

    public DeviceSummary toDeviceSummary(Device device) {
        if (device == null) {
            return null;
        }

        return DeviceSummary.builder()
                .id(device.getId())
                .name(device.getName())
                .imagePath(device.getImagePath())
                .build();
    }
}

