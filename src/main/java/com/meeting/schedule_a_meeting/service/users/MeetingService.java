
package com.meeting.schedule_a_meeting.service.users;

import com.meeting.schedule_a_meeting.dto.request.users.meetting.*;
import com.meeting.schedule_a_meeting.dto.response.users.meeting.*;
import com.meeting.schedule_a_meeting.entities.*;
import com.meeting.schedule_a_meeting.enums.*;
import com.meeting.schedule_a_meeting.exception.AppException;
import com.meeting.schedule_a_meeting.mapper.users.MeetingMapper;
import com.meeting.schedule_a_meeting.repositories.*;
import com.meeting.schedule_a_meeting.repositories.meeting.*;
import com.meeting.schedule_a_meeting.service.users.EmailService;

import com.meeting.schedule_a_meeting.repositories.meeting.MeetingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.meeting.schedule_a_meeting.dto.request.users.meetting.DeviceBorrowRequest;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.meeting.schedule_a_meeting.dto.response.admin.RoomDeviceResponse;

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
    private final EmailService emailService;


    private static final LocalTime MORNING_START = LocalTime.of(7, 0);
    private static final LocalTime MORNING_END = LocalTime.of(12, 0);
    private static final LocalTime AFTERNOON_START = LocalTime.of(13, 0);
    private static final LocalTime EVENING_END = LocalTime.of(23, 0);

    /* ====================== CREATE MEETING ====================== */
    @Transactional
    public MeetingResponse createMeeting(CreateMeetingRequest request, UUID creatorId) {
        validateMeetingTime(request.getStartTime(), request.getEndTime());
        validateRoomAvailability(request.getRoomId(), request.getStartTime(), request.getEndTime(), null);

        Users creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new AppException(ErrorStatus.USER_NOT_FOUND));

        MeetingRoom room = meetingRoomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new AppException(ErrorStatus.ROOM_NOT_FOUND));

        Meeting meeting = Meeting.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .meetingRoom(room)
                .creator(creator)
                .status(MeetingStatus.SCHEDULED)
                .build();

        final Meeting savedMeeting = meetingRepository.save(meeting);

        assignDefaultRoomDevices(meeting);

        addParticipantsByEmail(meeting, request.getParticipants(), creatorId);

        if (request.getBorrowedDevices() != null && !request.getBorrowedDevices().isEmpty()) {
            request.getBorrowedDevices().forEach(deviceReq -> borrowAdditionalDevice(savedMeeting, deviceReq));
        }

        return meetingMapper.toMeetingResponse(meeting);
    }

    /* ====================== UPDATE MEETING ====================== */
    @Transactional
    public MeetingResponse updateMeeting(String meetingId, UpdateMeetingRequest request, UUID userId) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new AppException(ErrorStatus.MEETING_NOT_FOUND));

        if (!meeting.getCreator().getId().equals(userId)) {
            throw new AppException(ErrorStatus.MEETING_CREATOR_REQUIRED);
        }
        if (meeting.getStatus() == MeetingStatus.CANCELLED) {
            throw new AppException(ErrorStatus.MEETING_ALREADY_CANCELLED);
        }
        if (meeting.getStartTime().isBefore(LocalDateTime.now().plusMinutes(1))) {
            throw new AppException(ErrorStatus.MEETING_CANNOT_EDIT_PAST);
        }

        // Update title + description
        if (request.getTitle() != null)
            meeting.setTitle(request.getTitle());
        if (request.getDescription() != null)
            meeting.setDescription(request.getDescription());

        // Update time
        if (request.getStartTime() != null && request.getEndTime() != null) {
            validateMeetingTime(request.getStartTime(), request.getEndTime());
            validateRoomAvailability(
                    request.getRoomId() != null ? request.getRoomId() : meeting.getMeetingRoom().getId(),
                    request.getStartTime(), request.getEndTime(), meetingId);
            meeting.setStartTime(request.getStartTime());
            meeting.setEndTime(request.getEndTime());
        }

        // Update room
        if (request.getRoomId() != null && !request.getRoomId().equals(meeting.getMeetingRoom().getId())) {
            MeetingRoom newRoom = meetingRoomRepository.findById(request.getRoomId())
                    .orElseThrow(() -> new AppException(ErrorStatus.ROOM_NOT_FOUND));
            validateRoomAvailability(newRoom.getId(), meeting.getStartTime(), meeting.getEndTime(), meetingId);
            meeting.setMeetingRoom(newRoom);
        }

        // Update participants
        if (request.getParticipants() != null) {
            updateParticipants(meeting, request.getParticipants(), userId);
        }

        // Update borrowed devices
        if (request.getBorrowedDevices() != null) {
            updateBorrowedDevices(meeting, request.getBorrowedDevices());
        }

        Meeting saved = meetingRepository.save(meeting);

        /*GỬI EMAIL CHO TẤT CẢ NGƯỜI THAM DỰ + NGƯỜI TẠO*/
        saved.getParticipants().forEach(mp -> {
            emailService.sendEmailMeetingUpdated(
                    mp.getUser().getEmail(),                    // to
                    saved.getTitle(),                           // meetingTitle
                    saved.getDescription(),                     // description
                    saved.getStartTime().toString(),            // start
                    saved.getEndTime().toString(),              // end
                    saved.getMeetingRoom().getName(),           // room
                    saved.getCreator().getName(),               // updatedBy
                    saved.getCreator().getEmail()               // updatedByEmail
            );
        });
        emailService.sendEmailMeetingUpdated(
                saved.getCreator().getEmail(),
                saved.getTitle(),
                saved.getDescription(),
                saved.getStartTime().toString(),
                saved.getEndTime().toString(),
                saved.getMeetingRoom().getName(),
                saved.getCreator().getName(),
                saved.getCreator().getEmail()
        );
        //

        return meetingMapper.toMeetingResponse(saved);
    }


    public List<RoomDeviceResponse> getRoomDevices(String roomId) {
        return roomDeviceRepository.findActiveDevicesByRoom(roomId).stream()
                .map(rd -> RoomDeviceResponse.builder()
                        .id(rd.getId())
                        .deviceName(rd.getDevice().getName())
                        .roomName(rd.getMeetingRoom().getName())
                        .quantity(rd.getQuantity())
                        .build())
                .collect(Collectors.toList());
    }

    public List<UserSummary> searchUsersByEmail(String emailPart) {
        return userRepository.findTop10ByEmailContainingIgnoreCase(emailPart).stream()
                .map(user -> UserSummary.builder()
                        .id(user.getId())
                        .name(user.getName())
                        .email(user.getEmail())
                        .avatarUrl(user.getAvatar_url())
                        .build())
                .collect(Collectors.toList());
    }

    public List<DeviceResponse> getAllActiveDevices() {
        return deviceRepository.findByStatus(DeviceStatus.ACTIVE).stream()
                .map(device -> DeviceResponse.builder()
                        .id(device.getId())
                        .device(DeviceSummary.builder()
                                .id(device.getId())
                                .name(device.getName())
                                .imagePath(device.getImagePath())
                                .build())

                        .status(MeetingDeviceStatus.valueOf(device.getStatus().name()))

                        .quantity(device.getTotalQuantity())
                        .availableQuantity(device.getAvailableQuantity())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteMeeting(String meetingId, UUID currentUserId) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new AppException(ErrorStatus.MEETING_NOT_FOUND));

        if (!meeting.getCreator().getId().equals(currentUserId)) {
            throw new AppException(ErrorStatus.MEETING_CREATOR_REQUIRED);
        }

        if (meeting.getStatus() == MeetingStatus.CANCELLED) {
            throw new AppException(ErrorStatus.MEETING_ALREADY_CANCELLED);
        }

        participantRepository.deleteByMeetingId(meetingId);
        meetingDeviceRepository.deleteByMeetingId(meetingId);

        meetingRepository.delete(meeting);
    }

    /* ====================== CANCEL MEETING ====================== */
    @Transactional
    public void cancelMeeting(String meetingId, UUID userId, String reason) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new AppException(ErrorStatus.MEETING_NOT_FOUND));

        if (!meeting.getCreator().getId().equals(userId)) {
            throw new AppException(ErrorStatus.MEETING_CREATOR_REQUIRED);
        }
        if (meeting.getStatus() == MeetingStatus.CANCELLED) {
            throw new AppException(ErrorStatus.MEETING_ALREADY_CANCELLED);
        }

        meeting.setStatus(MeetingStatus.CANCELLED);

        returnBorrowedDevices(meeting);
    }

    /* ====================== GET METHODS ====================== */
    public MeetingResponse getMeetingById(String meetingId, UUID userId) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new AppException(ErrorStatus.MEETING_NOT_FOUND));
        if (!hasAccess(meeting, userId)) {
            throw new AppException(ErrorStatus.FORBIDDEN);
        }
        return meetingMapper.toMeetingResponse(meeting);
    }

    public List<MeetingResponse> getMyMeetings(UUID userId) {
        return meetingRepository.findMeetingsByUser(userId).stream()
                .map(meetingMapper::toMeetingResponse)
                .toList();
    }

    public List<MeetingResponse> getMyCreatedMeetings(UUID userId) {
        return meetingRepository.findByCreatorIdOrderByStartTimeDesc(userId).stream()
                .map(meetingMapper::toMeetingResponse)
                .toList();
    }

    public List<MeetingResponse> getRoomSchedule(String roomId, LocalDateTime startDate, LocalDateTime endDate) {
        return meetingRepository.findMeetingsByRoomAndDateRange(roomId, startDate, endDate).stream()
                .map(meetingMapper::toMeetingResponse)
                .toList();
    }

    /* ====================== PRIVATE HELPERS ====================== */
    private void assignDefaultRoomDevices(Meeting meeting) {
        roomDeviceRepository.findByMeetingRoomId(meeting.getMeetingRoom().getId())
                .stream()
                .filter(rd -> rd.getStatus() == RoomDeviceStatus.IN_USE)
                .forEach(rd -> {
                    MeetingDevice md = MeetingDevice.builder()
                            .meeting(meeting)
                            .device(rd.getDevice())
                            .quantity(rd.getQuantity())
                            .status(MeetingDeviceStatus.RESERVED)
                            .notes("Auto-assigned from room")
                            .build();
                    meetingDeviceRepository.save(md);
                    meeting.getDevices().add(md);
                });
    }

    private void addParticipantsByEmail(Meeting meeting, List<ParticipantRequest> requests, UUID creatorId) {
        if (requests == null)
            return;
        requests.forEach(req -> {
            Users user = userRepository.findByEmail(req.getEmail())
                    .orElseThrow(() -> new AppException(ErrorStatus.USER_NOT_FOUND));
            if (user.getId().equals(creatorId))
                return;

            String meetingId = meeting.getId();
            UUID userId = user.getId();
            participantRepository.existsByMeetingIdAndUserId(meetingId, userId);
            if (participantRepository.existsByMeetingIdAndUserId(meetingId, userId)) {
                throw new AppException(ErrorStatus.PARTICIPANT_ALREADY_INVITED);
            }
            MeetingParticipant mp = MeetingParticipant.builder()
                    .meeting(meeting).user(user)
                    .role(req.getRole() != null ? req.getRole() : ParticipantRole.REQUIRED)
                    .status(ParticipantStatus.PENDING).invitedAt(LocalDateTime.now())
                    .build();
            participantRepository.save(mp);
            meeting.getParticipants().add(mp);
        });
    }

    private void borrowAdditionalDevice(Meeting meeting, DeviceBorrowRequest req) {
        Device device = deviceRepository.findById(req.getDeviceId())
                .orElseThrow(() -> new AppException(ErrorStatus.DEVICE_NOT_FOUND));
        if (device.getStatus() != DeviceStatus.ACTIVE)
            throw new AppException(ErrorStatus.DEVICE_NOT_ACTIVE);

        boolean alreadyInRoom = roomDeviceRepository.existsByMeetingRoomIdAndDeviceId(
                meeting.getMeetingRoom().getId(), req.getDeviceId());
        if (alreadyInRoom)
            throw new AppException(ErrorStatus.DEVICE_ALREADY_IN_USE_IN_ROOM);

        int reserved = meetingDeviceRepository.getTotalReservedQuantity(
                req.getDeviceId(), meeting.getStartTime(), meeting.getEndTime());
        if (device.getAvailableQuantity() < req.getQuantity()) {
            throw new AppException(ErrorStatus.INSUFFICIENT_QUANTITY);
        }

        device.setAvailableQuantity(device.getAvailableQuantity() - req.getQuantity());
        deviceRepository.save(device);

        MeetingDevice md = MeetingDevice.builder()
                .meeting(meeting).device(device).quantity(req.getQuantity())
                .status(MeetingDeviceStatus.RESERVED).notes(req.getNotes())
                .build();
        meetingDeviceRepository.save(md);
        meeting.getDevices().add(md);
    }

    private void updateBorrowedDevices(Meeting meeting, List<DeviceBorrowRequest> newList) {
        List<MeetingDevice> currentBorrowed = meeting.getDevices().stream()
                .filter(md -> !roomDeviceRepository.existsByMeetingRoomIdAndDeviceId(
                        meeting.getMeetingRoom().getId(), md.getDevice().getId()))
                .toList();

        currentBorrowed.forEach(md -> {
            Device d = md.getDevice();
            d.setAvailableQuantity(d.getAvailableQuantity() + md.getQuantity());
            deviceRepository.save(d);
            meetingDeviceRepository.delete(md);
            meeting.getDevices().remove(md);
        });

        newList.forEach(req -> borrowAdditionalDevice(meeting, req));
    }

    private void returnBorrowedDevices(Meeting meeting) {
        meeting.getDevices().stream()
                .filter(md -> !roomDeviceRepository.existsByMeetingRoomIdAndDeviceId(
                        meeting.getMeetingRoom().getId(), md.getDevice().getId()))
                .forEach(md -> {
                    Device d = md.getDevice();
                    d.setAvailableQuantity(d.getAvailableQuantity() + md.getQuantity());
                    deviceRepository.save(d);
                });
    }

    private void updateParticipants(Meeting meeting, List<ParticipantRequest> requests, UUID creatorId) {
        participantRepository.deleteByMeetingId(meeting.getId());
        meeting.getParticipants().clear();
        addParticipantsByEmail(meeting, requests, creatorId);
    }

    private boolean hasAccess(Meeting meeting, UUID userId) {
        return meeting.getCreator().getId().equals(userId) ||
                participantRepository.existsByMeetingIdAndUserId(meeting.getId(), userId);
    }

    private void validateMeetingTime(LocalDateTime start, LocalDateTime end) {
        if (end.isBefore(start) || end.isEqual(start))
            throw new AppException(ErrorStatus.MEETING_END_BEFORE_START);
        LocalTime st = start.toLocalTime(), et = end.toLocalTime();
        boolean morning = !st.isBefore(MORNING_START) && !et.isAfter(MORNING_END);
        boolean afternoon = !st.isBefore(AFTERNOON_START) && !et.isAfter(EVENING_END);
        if (!(morning || afternoon))
            throw new AppException(ErrorStatus.MEETING_INVALID_TIME_RANGE);
    }

    private void validateRoomAvailability(String roomId, LocalDateTime start, LocalDateTime end, String excludeId) {
        List<Meeting> conflicts = excludeId == null
                ? meetingRepository.findConflictingMeetings(roomId, start, end)
                : meetingRepository.findConflictingMeetingsExcludingCurrent(roomId, excludeId, start, end);
        if (!conflicts.isEmpty())
            throw new AppException(ErrorStatus.MEETING_ROOM_NOT_AVAILABLE);
    }
}