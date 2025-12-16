package com.meeting.schedule_a_meeting.mapper.users;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.meeting.schedule_a_meeting.dto.response.users.meeting.DeviceResponse;
import com.meeting.schedule_a_meeting.dto.response.users.meeting.DeviceSummary;
import com.meeting.schedule_a_meeting.dto.response.users.meeting.MeetingResponse;
import com.meeting.schedule_a_meeting.dto.response.users.meeting.ParticipantResponse;
import com.meeting.schedule_a_meeting.dto.response.users.meeting.RoomSummary;
import com.meeting.schedule_a_meeting.dto.response.users.meeting.UserSummary;
import com.meeting.schedule_a_meeting.entities.Device;
import com.meeting.schedule_a_meeting.entities.Meeting;
import com.meeting.schedule_a_meeting.entities.MeetingDevice;
import com.meeting.schedule_a_meeting.entities.MeetingParticipant;
import com.meeting.schedule_a_meeting.entities.MeetingRoom;
import com.meeting.schedule_a_meeting.entities.Users;
import com.meeting.schedule_a_meeting.repositories.meeting.MeetingRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MeetingMapper {

    private final MeetingRepository meetingRepository;

    public MeetingResponse toMeetingResponse(Meeting meeting) {
        if (meeting == null)
            return null;
        boolean hasConcluded = meeting.getEndTime() != null &&
                meeting.getEndTime().isBefore(LocalDateTime.now());

        String repeatSummary = null;
        if (meeting.isRepeat() && meeting.getRepeatDays() != null && !meeting.getRepeatDays().isEmpty()) {
            String[] days = meeting.getRepeatDays().split(",");
            List<String> dayNames = Arrays.stream(days)
                    .map(String::trim)
                    .map(this::getVietnameseDayName)
                    .collect(Collectors.toList());

            if (days.length == 6 && Arrays.asList(days).containsAll(
                    List.of("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"))) {
                repeatSummary = "Daily (Monday - Saturday)";
            } else if (days.length == 1) {
                // Là weekly: đếm số meeting trong group để biết lặp bao nhiêu tuần
                long repeatCount = meeting.getRepeatGroupId() != null
                        ? meetingRepository.countByRepeatGroupId(meeting.getRepeatGroupId())
                        : 1;
                repeatSummary = "Every Weeks (" + dayNames.get(0) + ") - " + repeatCount + " weeks";
            } else {
                repeatSummary = "Every " + String.join(", ", dayNames);
            }
        }

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
                .isRepeat(meeting.isRepeat())
                .room(toRoomSummary(meeting.getMeetingRoom()))
                .creator(toUserSummary(meeting.getCreator()))
                .participants(participants)
                .devices(devices)
                .createdAt(meeting.getCreatedAt())
                .updatedAt(meeting.getUpdatedAt())
                .hasConcluded(hasConcluded)
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

    private String getVietnameseDayName(String englishDay) {
        return switch (englishDay.toUpperCase()) {
            case "MONDAY" -> "MONDAY";
            case "TUESDAY" -> "TUESDAY";
            case "WEDNESDAY" -> "WEDNESDAY";
            case "THURSDAY" -> "THURSDAY";
            case "FRIDAY" -> "FRIDAY";
            case "SATURDAY" -> "SATURDAY";
            case "SUNDAY" -> "SUNDAY";
            default -> englishDay;
        };
    }
}
