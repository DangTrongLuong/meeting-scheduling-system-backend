package com.meeting.schedule_a_meeting.service.users;



import com.meeting.schedule_a_meeting.dto.request.users.meetting.CreateMeetingRequest;
import com.meeting.schedule_a_meeting.dto.request.users.meetting.DeviceRequest;
import com.meeting.schedule_a_meeting.dto.request.users.meetting.ParticipantRequest;
import com.meeting.schedule_a_meeting.dto.request.users.meetting.UpdateMeetingRequest;
import com.meeting.schedule_a_meeting.dto.response.users.meeting.MeetingResponse;
import com.meeting.schedule_a_meeting.entities.*;
import com.meeting.schedule_a_meeting.enums.*;
import com.meeting.schedule_a_meeting.exception.AppException;

import com.meeting.schedule_a_meeting.mapper.users.MeetingMapper;
import com.meeting.schedule_a_meeting.repositories.*;
import com.meeting.schedule_a_meeting.repositories.meeting.MeetingDeviceRepository;
import com.meeting.schedule_a_meeting.repositories.meeting.MeetingParticipantRepository;
import com.meeting.schedule_a_meeting.repositories.meeting.MeetingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MeetingService {

    private final MeetingRepository meetingRepository;
    private final MeetingParticipantRepository participantRepository;
    private final MeetingDeviceRepository meetingDeviceRepository;
    private final UserRepository userRepository;
    private final MeetingRoomRepository meetingRoomRepository;
    private final DeviceRepository deviceRepository;
    private final RoomDeviceRepository roomDeviceRepository;
    private final MeetingMapper meetingMapper;

    // Thời gian làm việc: 7h-12h và 13h-23h
    private static final LocalTime MORNING_START = LocalTime.of(7, 0);
    private static final LocalTime MORNING_END = LocalTime.of(12, 0);
    private static final LocalTime AFTERNOON_START = LocalTime.of(13, 0);
    private static final LocalTime EVENING_END = LocalTime.of(23, 0);

    @Transactional
    public MeetingResponse createMeeting(CreateMeetingRequest request, UUID creatorId) {
        log.info("Creating meeting: {} by user: {}", request.getTitle(), creatorId);


        validateMeetingTime(request.getStartTime(), request.getEndTime());


        Users creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new AppException(ErrorStatus.USER_NOT_FOUND));

        MeetingRoom room = meetingRoomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new AppException(ErrorStatus.ROOM_NOT_FOUND));


        checkRoomAvailability(request.getRoomId(), request.getStartTime(), request.getEndTime(), null);


        Meeting meeting = Meeting.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .meetingRoom(room)
                .creator(creator)
                .status(MeetingStatus.SCHEDULED)
                .build();

        meeting = meetingRepository.save(meeting);


        addParticipant(meeting, creator, ParticipantRole.ORGANIZER);


        if (request.getParticipants() != null && !request.getParticipants().isEmpty()) {
            for (ParticipantRequest participantRequest : request.getParticipants()) {

                if (participantRequest.getUserId().equals(creatorId)) {
                    continue;
                }

                Users user = userRepository.findById(participantRequest.getUserId())
                        .orElseThrow(() -> new AppException(ErrorStatus.USER_NOT_FOUND));


                if (participantRepository.existsByMeetingIdAndUserId(meeting.getId(), user.getId())) {
                    throw new AppException(ErrorStatus.PARTICIPANT_ALREADY_INVITED,
                            "User " + user.getEmail() + " is already invited");
                }

                addParticipant(meeting, user, participantRequest.getRole());
            }
        }


        if (request.getDevices() != null && !request.getDevices().isEmpty()) {
            for (DeviceRequest deviceRequest : request.getDevices()) {
                addDevice(meeting, deviceRequest, request.getStartTime(), request.getEndTime());
            }
        }


        meeting = meetingRepository.findById(meeting.getId())
                .orElseThrow(() -> new AppException(ErrorStatus.MEETING_NOT_FOUND));

        log.info("Meeting created successfully: {}", meeting.getId());
        return meetingMapper.toMeetingResponse(meeting);
    }

    @Transactional(readOnly = true)
    public MeetingResponse getMeetingById(String meetingId, UUID userId) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new AppException(ErrorStatus.MEETING_NOT_FOUND));

        // Check if user has access (creator or participant)
        if (!hasAccessToMeeting(meeting, userId)) {
            throw new AppException(ErrorStatus.FORBIDDEN);
        }

        return meetingMapper.toMeetingResponse(meeting);
    }

    @Transactional(readOnly = true)
    public List<MeetingResponse> getMyMeetings(UUID userId) {
        List<Meeting> meetings = meetingRepository.findMeetingsByUser(userId);
        return meetings.stream()
                .map(meetingMapper::toMeetingResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MeetingResponse> getMyCreatedMeetings(UUID userId) {
        List<Meeting> meetings = meetingRepository.findByCreatorIdOrderByStartTimeDesc(userId);
        return meetings.stream()
                .map(meetingMapper::toMeetingResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MeetingResponse> getRoomSchedule(String roomId, LocalDateTime startDate, LocalDateTime endDate) {
        // Check room exists
        if (!meetingRoomRepository.existsById(roomId)) {
            throw new AppException(ErrorStatus.ROOM_NOT_FOUND);
        }

        List<Meeting> meetings = meetingRepository.findMeetingsByRoomAndDateRange(roomId, startDate, endDate);
        return meetings.stream()
                .map(meetingMapper::toMeetingResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public MeetingResponse updateMeeting(String meetingId, UpdateMeetingRequest request, UUID userId) {
        log.info("Updating meeting: {} by user: {}", meetingId, userId);

        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new AppException(ErrorStatus.MEETING_NOT_FOUND));

        // Check if user is creator
        if (!meeting.getCreator().getId().equals(userId)) {
            throw new AppException(ErrorStatus.MEETING_CREATOR_REQUIRED);
        }

        // Check if meeting is cancelled
        if (meeting.getStatus() == MeetingStatus.CANCELLED) {
            throw new AppException(ErrorStatus.MEETING_ALREADY_CANCELLED);
        }

        // Check if meeting is in the past
        if (meeting.getStartTime().isBefore(LocalDateTime.now())) {
            throw new AppException(ErrorStatus.MEETING_CANNOT_EDIT_PAST);
        }

        boolean needsNotification = false;

        // Update basic info
        if (request.getTitle() != null && !request.getTitle().isEmpty()) {
            meeting.setTitle(request.getTitle());
        }

        if (request.getDescription() != null) {
            meeting.setDescription(request.getDescription());
        }

        // Update time
        if (request.getStartTime() != null && request.getEndTime() != null) {
            validateMeetingTime(request.getStartTime(), request.getEndTime());

            String newRoomId = request.getRoomId() != null ? request.getRoomId() : meeting.getMeetingRoom().getId();
            checkRoomAvailability(newRoomId, request.getStartTime(), request.getEndTime(), meetingId);

            meeting.setStartTime(request.getStartTime());
            meeting.setEndTime(request.getEndTime());
            needsNotification = true;
        }

        // Update room
        if (request.getRoomId() != null && !request.getRoomId().equals(meeting.getMeetingRoom().getId())) {
            MeetingRoom newRoom = meetingRoomRepository.findById(request.getRoomId())
                    .orElseThrow(() -> new AppException(ErrorStatus.ROOM_NOT_FOUND));

            checkRoomAvailability(request.getRoomId(), meeting.getStartTime(), meeting.getEndTime(), meetingId);

            meeting.setMeetingRoom(newRoom);
            needsNotification = true;
        }

        // Update participants
        if (request.getParticipants() != null) {
            updateParticipants(meeting, request.getParticipants(), userId);
        }

        // Update devices
        if (request.getDevices() != null) {
            updateDevices(meeting, request.getDevices());
        }

        meeting = meetingRepository.save(meeting);


        if (needsNotification) {
            log.info("Meeting updated, notification needed for meeting: {}", meetingId);
            // sendUpdateNotification(meeting);
        }

        log.info("Meeting updated successfully: {}", meetingId);
        return meetingMapper.toMeetingResponse(meeting);
    }

    @Transactional
    public void cancelMeeting(String meetingId, UUID userId, String reason) {
        log.info("Cancelling meeting: {} by user: {}", meetingId, userId);

        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new AppException(ErrorStatus.MEETING_NOT_FOUND));

        // Check if user is creator
        if (!meeting.getCreator().getId().equals(userId)) {
            throw new AppException(ErrorStatus.MEETING_CREATOR_REQUIRED);
        }

        // Check if already cancelled
        if (meeting.getStatus() == MeetingStatus.CANCELLED) {
            throw new AppException(ErrorStatus.MEETING_ALREADY_CANCELLED);
        }

        meeting.setStatus(MeetingStatus.CANCELLED);
        meeting.setCancelledAt(LocalDateTime.now());
        meeting.setCancellationReason(reason);

        meetingRepository.save(meeting);

        // TODO: Send cancellation notification
        log.info("Meeting cancelled successfully: {}", meetingId);
        // sendCancellationNotification(meeting);
    }

    // ============= PRIVATE HELPER METHODS =============

    private void validateMeetingTime(LocalDateTime startTime, LocalDateTime endTime) {
        // Check end time after start time
        if (!endTime.isAfter(startTime)) {
            throw new AppException(ErrorStatus.MEETING_END_BEFORE_START);
        }

        // Check time is in working hours
        LocalTime startLocalTime = startTime.toLocalTime();
        LocalTime endLocalTime = endTime.toLocalTime();

        boolean isValidMorning = !startLocalTime.isBefore(MORNING_START) && !endLocalTime.isAfter(MORNING_END);
        boolean isValidAfternoon = !startLocalTime.isBefore(AFTERNOON_START) && !endLocalTime.isAfter(EVENING_END);

        if (!isValidMorning && !isValidAfternoon) {
            throw new AppException(ErrorStatus.MEETING_INVALID_TIME_RANGE,
                    "Meeting time must be within 7:00-12:00 or 13:00-23:00");
        }

        // Check if meeting spans across lunch break
        if (startLocalTime.isBefore(MORNING_END) && endLocalTime.isAfter(AFTERNOON_START)) {
            throw new AppException(ErrorStatus.MEETING_INVALID_TIME_RANGE,
                    "Meeting cannot span across lunch break (12:00-13:00)");
        }
    }

    private void checkRoomAvailability(String roomId, LocalDateTime startTime, LocalDateTime endTime, String excludeMeetingId) {
        List<Meeting> conflicts;

        if (excludeMeetingId != null) {
            conflicts = meetingRepository.findConflictingMeetingsExcludingCurrent(
                    roomId, excludeMeetingId, startTime, endTime);
        } else {
            conflicts = meetingRepository.findConflictingMeetings(roomId, startTime, endTime);
        }

        if (!conflicts.isEmpty()) {
            Meeting conflictMeeting = conflicts.get(0);
            throw new AppException(ErrorStatus.MEETING_TIME_CONFLICT,
                    String.format("Room is already booked from %s to %s for meeting: %s",
                            conflictMeeting.getStartTime(),
                            conflictMeeting.getEndTime(),
                            conflictMeeting.getTitle()));
        }
    }

    private void addParticipant(Meeting meeting, Users user, ParticipantRole role) {
        MeetingParticipant participant = MeetingParticipant.builder()
                .meeting(meeting)
                .user(user)
                .role(role)
                .status(role == ParticipantRole.ORGANIZER ? ParticipantStatus.ACCEPTED : ParticipantStatus.PENDING)
                .build();

        participantRepository.save(participant);
        meeting.addParticipant(participant);

        // TODO: Send invitation email if not organizer
        if (role != ParticipantRole.ORGANIZER) {
            log.info("Invitation email should be sent to: {}", user.getEmail());
            // sendInvitationEmail(participant);
        }
    }

    private void addDevice(Meeting meeting, DeviceRequest request, LocalDateTime startTime, LocalDateTime endTime) {
        Device device = deviceRepository.findById(request.getDeviceId())
                .orElseThrow(() -> new AppException(ErrorStatus.DEVICE_NOT_FOUND));

        // Check if device is in the room
        boolean deviceInRoom = roomDeviceRepository
                .existsByMeetingRoomIdAndDeviceId(meeting.getMeetingRoom().getId(), device.getId());

        if (!deviceInRoom) {
            throw new AppException(ErrorStatus.DEVICE_NOT_IN_ROOM,
                    "Device " + device.getName() + " is not available in this room");
        }

        // Check device availability
        int reservedQuantity = meetingDeviceRepository.getTotalReservedQuantity(
                device.getId(), startTime, endTime);
        int availableQuantity = device.getAvailableQuantity() - reservedQuantity;

        if (availableQuantity < request.getQuantity()) {
            throw new AppException(ErrorStatus.DEVICE_NOT_AVAILABLE,
                    String.format("Only %d units of %s available, but %d requested",
                            availableQuantity, device.getName(), request.getQuantity()));
        }

        MeetingDevice meetingDevice = MeetingDevice.builder()
                .meeting(meeting)
                .device(device)
                .quantity(request.getQuantity())
                .notes(request.getNotes())
                .status(MeetingDeviceStatus.RESERVED)
                .build();

        meetingDeviceRepository.save(meetingDevice);
        meeting.addDevice(meetingDevice);
    }

    private void updateParticipants(Meeting meeting, List<ParticipantRequest> newParticipants, UUID creatorId) {
        // Remove old participants (except creator/organizer)
        List<MeetingParticipant> oldParticipants = participantRepository.findByMeetingId(meeting.getId());
        for (MeetingParticipant old : oldParticipants) {
            if (old.getRole() != ParticipantRole.ORGANIZER) {
                participantRepository.delete(old);
                meeting.removeParticipant(old);
            }
        }

        // Add new participants
        for (ParticipantRequest request : newParticipants) {
            if (request.getUserId().equals(creatorId)) {
                continue; // Skip creator
            }

            Users user = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new AppException(ErrorStatus.USER_NOT_FOUND));

            addParticipant(meeting, user, request.getRole());
        }
    }

    private void updateDevices(Meeting meeting, List<DeviceRequest> newDevices) {
        // Remove old devices
        List<MeetingDevice> oldDevices = meetingDeviceRepository.findByMeetingId(meeting.getId());
        for (MeetingDevice old : oldDevices) {
            meetingDeviceRepository.delete(old);
            meeting.removeDevice(old);
        }

        // Add new devices
        for (DeviceRequest request : newDevices) {
            addDevice(meeting, request, meeting.getStartTime(), meeting.getEndTime());
        }
    }

    private boolean hasAccessToMeeting(Meeting meeting, UUID userId) {
        // Creator has access
        if (meeting.getCreator().getId().equals(userId)) {
            return true;
        }

        // Check if user is participant
        return participantRepository.existsByMeetingIdAndUserId(meeting.getId(), userId);
    }
}