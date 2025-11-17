package com.meeting.schedule_a_meeting.service.users;

import com.meeting.schedule_a_meeting.dto.request.users.CreateMeetingRequest;
import com.meeting.schedule_a_meeting.dto.request.users.MeetingRoomScheduleRequest;
import com.meeting.schedule_a_meeting.dto.response.users.MeetingResponse;
import com.meeting.schedule_a_meeting.dto.response.users.TimeSlot;
import com.meeting.schedule_a_meeting.entities.Meeting;
import com.meeting.schedule_a_meeting.entities.MeetingRoom;
import com.meeting.schedule_a_meeting.repositories.MeetingRepository;
import com.meeting.schedule_a_meeting.repositories.MeetingRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MeetingService {

    private final MeetingRepository meetingRepository;
    private final MeetingRoomRepository meetingRoomRepository;

    // ✅ Create a meeting with conflict check
    public MeetingResponse createMeeting(CreateMeetingRequest request, String createdBy) {
        if (request.getStartTime().isAfter(request.getEndTime())) {
            throw new IllegalArgumentException("Start time must be before end time");
        }

        MeetingRoom room = meetingRoomRepository.findById(request.getRoomId())
                .orElseThrow(
                        () -> new IllegalArgumentException("Không tìm thấy phòng họp với ID: " + request.getRoomId()));
        // Kiểm tra trùng phòng
        List<Meeting> conflicts = meetingRepository
                .findByRoomAndStartTimeLessThanEqualAndEndTimeGreaterThanEqual(
                        room, request.getEndTime(), request.getStartTime());

        boolean hasConflict = conflicts.stream()
                .anyMatch(m -> request.getStartTime().isBefore(m.getEndTime()) &&
                        request.getEndTime().isAfter(m.getStartTime()));

        if (hasConflict) {
            Meeting conflictMeeting = conflicts.get(0);
            throw new IllegalArgumentException("Room is already booked from " +
                    conflictMeeting.getStartTime() + " to " + conflictMeeting.getEndTime());
        }

        Meeting meeting = Meeting.builder()
                .title(request.getTitle())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .room(room)
                .createdBy(createdBy)
                .status(request.getStatus()) // Ensure status exists in DTO & Entity
                .invitedEmails(request.getInvitedEmails() != null ? request.getInvitedEmails() : Collections.emptyList())
                .build();

        Meeting saved = meetingRepository.save(meeting);

        return MeetingResponse.builder()
                .id(saved.getId())
                .title(saved.getTitle())
                .startTime(saved.getStartTime())
                .endTime(saved.getEndTime())
                .roomName(saved.getRoom().getName())
                .createdBy(saved.getCreatedBy())
                .status(saved.getStatus())
                .invitedEmails(saved.getInvitedEmails())
                .build();
    }

    // ✅ Get meeting room schedule (30-min slots from 08:00 to 18:00)
    public List<TimeSlot> getMeetingRoomSchedule(MeetingRoomScheduleRequest request) {
        MeetingRoom room = meetingRoomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new IllegalArgumentException("Meeting room not found"));

        LocalDateTime dayStart = request.getStartDate().atStartOfDay();
        LocalDateTime dayEnd = dayStart.plusDays(1);

        List<Meeting> bookedMeetings = meetingRepository
                .findByRoomAndStartTimeLessThanEqualAndEndTimeGreaterThanEqual(room, dayStart, dayEnd);

        List<TimeSlot> slots = new ArrayList<>();
        LocalDateTime slotStart = dayStart.withHour(8);
        LocalDateTime slotEndLimit = dayStart.withHour(18);

        while (slotStart.isBefore(slotEndLimit)) {
            LocalDateTime slotEnd = slotStart.plusMinutes(30);

            LocalDateTime finalSlotStart = slotStart;
            Meeting meeting = bookedMeetings.stream()
                    .filter(m -> finalSlotStart.isBefore(m.getEndTime()) && slotEnd.isAfter(m.getStartTime()))
                    .findFirst()
                    .orElse(null);

            boolean isBooked = meeting != null;
            String title = isBooked ? meeting.getTitle() : "Available";

            slots.add(new TimeSlot(slotStart, slotEnd, isBooked, title));
            slotStart = slotEnd;
        }

        return slots;
    }

    // ✅ Cancel meeting
    public void cancelMeeting(Long meetingId, String createdBy) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new IllegalArgumentException("Meeting not found with ID: " + meetingId));

        if (!meeting.getCreatedBy().equals(createdBy)) {
            throw new IllegalArgumentException("You do not have permission to cancel this meeting");
        }

        meetingRepository.delete(meeting);
    }

    // ✅ Get meeting details
    public MeetingResponse getMeetingDetail(Long id) {
        Meeting meeting = meetingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Meeting not found"));
        return MeetingResponse.builder()
                .id(meeting.getId())
                .title(meeting.getTitle())
                .startTime(meeting.getStartTime())
                .endTime(meeting.getEndTime())
                .roomName(meeting.getRoom().getName())
                .createdBy(meeting.getCreatedBy())
                .invitedEmails(meeting.getInvitedEmails())
                .status(meeting.getStatus())
                .build();
    }
}
