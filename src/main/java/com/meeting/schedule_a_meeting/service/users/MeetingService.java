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

    // ✅ Tạo cuộc họp với kiểm tra trùng lịch chuẩn
    public MeetingResponse createMeeting(CreateMeetingRequest request, String createdBy) {
        if (request.getStartTime().isAfter(request.getEndTime())) {
            throw new IllegalArgumentException("Thời gian bắt đầu phải trước thời gian kết thúc");
        }

        MeetingRoom room = meetingRoomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng họp với ID: " + request.getRoomId()));

        // Lấy tất cả cuộc họp trong khoảng thời gian có thể trùng
        List<Meeting> conflicts = meetingRepository
                .findByRoomAndStartTimeLessThanEqualAndEndTimeGreaterThanEqual(
                        room, request.getEndTime(), request.getStartTime());

        // Kiểm tra overlap thực sự (không tính liền kề)
        boolean hasConflict = conflicts.stream()
                .anyMatch(m -> request.getStartTime().isBefore(m.getEndTime()) &&
                        request.getEndTime().isAfter(m.getStartTime()));

        if (hasConflict) {
            Meeting conflictMeeting = conflicts.get(0);
            throw new IllegalArgumentException("Phòng họp đã được đặt từ " +
                    conflictMeeting.getStartTime() + " đến " + conflictMeeting.getEndTime());
        }

        Meeting meeting = Meeting.builder()
                .title(request.getTitle())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .room(room)
                .createdBy(createdBy)
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
                .invitedEmails(saved.getInvitedEmails())
                .build();
    }

    // ✅ Lấy lịch phòng họp (slot 30 phút từ 08:00 đến 18:00)
    public List<TimeSlot> getMeetingRoomSchedule(MeetingRoomScheduleRequest request) {
        MeetingRoom room = meetingRoomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng họp"));

        LocalDateTime dayStart = request.getStartDate().atStartOfDay();
        LocalDateTime dayEnd = dayStart.plusDays(1);

        List<Meeting> bookedMeetings = meetingRepository
                .findByRoomAndStartTimeLessThanEqualAndEndTimeGreaterThanEqual(room, dayStart, dayEnd);

        List<TimeSlot> slots = new ArrayList<>();
        LocalDateTime slotStart = dayStart.withHour(8); // Bắt đầu từ 8h sáng
        LocalDateTime slotEndLimit = dayStart.withHour(18); // Kết thúc 18h

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
}