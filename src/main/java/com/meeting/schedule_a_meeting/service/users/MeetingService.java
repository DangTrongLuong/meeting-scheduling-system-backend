
package com.meeting.schedule_a_meeting.service.users;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meeting.schedule_a_meeting.dto.request.users.meetting.CreateMeetingRequest;
import com.meeting.schedule_a_meeting.dto.request.users.meetting.DeviceBorrowRequest;
import com.meeting.schedule_a_meeting.dto.request.users.meetting.ParticipantRequest;
import com.meeting.schedule_a_meeting.dto.request.users.meetting.UpdateMeetingRequest;
import com.meeting.schedule_a_meeting.dto.response.admin.RoomDeviceResponse;
import com.meeting.schedule_a_meeting.dto.response.users.meeting.DeviceResponse;
import com.meeting.schedule_a_meeting.dto.response.users.meeting.DeviceSummary;
import com.meeting.schedule_a_meeting.dto.response.users.meeting.MeetingResponse;
import com.meeting.schedule_a_meeting.dto.response.users.meeting.UserSummary;
import com.meeting.schedule_a_meeting.entities.Device;
import com.meeting.schedule_a_meeting.entities.Meeting;
import com.meeting.schedule_a_meeting.entities.MeetingDevice;
import com.meeting.schedule_a_meeting.entities.MeetingParticipant;
import com.meeting.schedule_a_meeting.entities.MeetingRoom;
import com.meeting.schedule_a_meeting.entities.Users;
import com.meeting.schedule_a_meeting.enums.DeviceStatus;
import com.meeting.schedule_a_meeting.enums.ErrorStatus;
import com.meeting.schedule_a_meeting.enums.MeetingDeviceStatus;
import com.meeting.schedule_a_meeting.enums.MeetingStatus;
import com.meeting.schedule_a_meeting.enums.ParticipantRole;
import com.meeting.schedule_a_meeting.enums.ParticipantStatus;
import com.meeting.schedule_a_meeting.enums.RoomDeviceStatus;
import com.meeting.schedule_a_meeting.exception.AppException;
import com.meeting.schedule_a_meeting.mapper.users.MeetingMapper;
import com.meeting.schedule_a_meeting.repositories.DeviceRepository;
import com.meeting.schedule_a_meeting.repositories.MeetingRoomRepository;
import com.meeting.schedule_a_meeting.repositories.RoomDeviceRepository;
import com.meeting.schedule_a_meeting.repositories.UserRepository;
import com.meeting.schedule_a_meeting.repositories.meeting.MeetingDeviceRepository;
import com.meeting.schedule_a_meeting.repositories.meeting.MeetingParticipantRepository;
import com.meeting.schedule_a_meeting.repositories.meeting.MeetingRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
    private final GoogleCalendarService googleCalendarService;

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
                .status(MeetingStatus.PENDING_APPROVAL)
                .createdBy(creator.getName())
                .build();

        meetingRepository.saveAndFlush(meeting);

        assignDefaultRoomDevices(meeting);
        addParticipantsByEmail(meeting, request.getParticipants(), creatorId);

        if (request.getBorrowedDevices() != null && !request.getBorrowedDevices().isEmpty()) {
            request.getBorrowedDevices().forEach(deviceReq -> borrowAdditionalDevice(meeting, deviceReq));
        }

        Meeting updatedMeeting = meetingRepository.findById(meeting.getId())
                .orElseThrow(() -> new AppException(ErrorStatus.MEETING_NOT_FOUND));

        return meetingMapper.toMeetingResponse(updatedMeeting);
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

        // ✅ Chỉ sync nếu meeting đã được duyệt
        if (saved.getStatus() == MeetingStatus.SCHEDULED && googleCalendarService.isConnected(userId)) {
            try {
                googleCalendarService.syncMeetingToGoogle(saved, userId);
            } catch (Exception e) {
                log.warn("Failed to sync meeting update to Google Calendar", e);
            }
        } else {
            log.info("Meeting {} not synced (status={}).", saved.getId(), saved.getStatus());
        }

        // Gửi email thông báo cập nhật
        saved.getParticipants().forEach(mp -> {
            emailService.sendEmailMeetingUpdated(
                    mp.getUser().getEmail(),
                    saved.getTitle(),
                    saved.getDescription(),
                    saved.getStartTime().toString(),
                    saved.getEndTime().toString(),
                    saved.getMeetingRoom().getName(),
                    saved.getCreator().getName(),
                    saved.getCreator().getEmail());
        });
        emailService.sendEmailMeetingUpdated(
                saved.getCreator().getEmail(),
                saved.getTitle(),
                saved.getDescription(),
                saved.getStartTime().toString(),
                saved.getEndTime().toString(),
                saved.getMeetingRoom().getName(),
                saved.getCreator().getName(),
                saved.getCreator().getEmail());

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
        meeting.setCancelledAt(LocalDateTime.now());
        meeting.setCancellationReason(reason);
        meetingRepository.save(meeting);

        UUID creatorId = meeting.getCreator().getId();

        boolean isConnected = googleCalendarService.isConnected(creatorId);
        if (isConnected && meeting.getGoogleEventId() != null && !meeting.getGoogleEventId().isBlank()) {
            try {
                googleCalendarService.deleteGoogleEventIfExists(meeting, creatorId);

                // Xóa ID khỏi DB để lần sau không thử xóa lại
                meeting.setGoogleEventId(null);
                meeting.setLastSyncedAt(null);
                meetingRepository.save(meeting);

                log.info("Successfully deleted Google Calendar event for cancelled meeting {}", meetingId);
            } catch (Exception e) {
                log.warn("Failed to delete Google Calendar event {} when cancelling meeting {}",
                        meeting.getGoogleEventId(), meetingId, e);
                // Không throw → vẫn cho hủy meeting bình thường
            }
        }

        returnBorrowedDevices(meeting);
        meeting.getParticipants().size(); // force load

        emailService.sendCancelMeetingEmail(meeting.getCreator().getEmail(), meeting);
        for (MeetingParticipant participant : meeting.getParticipants()) {
            emailService.sendCancelMeetingEmail(participant.getUser().getEmail(), meeting);
        }
    }

    /* ====================== GET METHODS ====================== */
    public MeetingResponse getMeetingById(String meetingId, UUID userId) {
        Meeting meeting = meetingRepository.findByIdWithParticipants(meetingId)
                .orElseThrow(() -> new AppException(ErrorStatus.MEETING_NOT_FOUND));

        if (!hasAccess(meeting, userId)) {
            throw new AppException(ErrorStatus.FORBIDDEN);
        }
        Meeting updatedMeeting = meetingRepository.findById(meeting.getId())
                .orElseThrow(() -> new AppException(ErrorStatus.MEETING_NOT_FOUND));

        return meetingMapper.toMeetingResponse(updatedMeeting);
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
        if (requests == null || requests.isEmpty())
            return;

        for (ParticipantRequest req : requests) {
            String email = req.getEmail();
            if (email == null || email.isBlank())
                continue;

            email = email.trim();

            Users user = userRepository.findByEmailIgnoreCase(email)
                    .orElseThrow(() -> new AppException(ErrorStatus.USER_NOT_FOUND));

            if (user.getId().equals(creatorId))
                continue;

            if (participantRepository.existsByMeetingIdAndUserId(meeting.getId(), user.getId())) {
                throw new AppException(ErrorStatus.PARTICIPANT_ALREADY_INVITED);
            }

            MeetingParticipant mp = MeetingParticipant.builder()
                    .meeting(meeting)
                    .user(user)
                    .guestEmail(email)
                    .role(req.getRole() != null ? req.getRole() : ParticipantRole.REQUIRED)
                    .status(ParticipantStatus.PENDING)
                    .invitedAt(LocalDateTime.now())
                    .build();
            participantRepository.save(mp);
        }
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

    @Transactional
    public void updateParticipantStatusByEmailAndMeeting(String email, String meetingId, ParticipantStatus status) {
        MeetingParticipant participant = participantRepository
                .findByMeetingIdAndUserEmail(meetingId, email)
                .orElseThrow(
                        () -> new AppException(ErrorStatus.PARTICIPANT_NOT_FOUND, "Invitation not found or invalid"));

        if (participant.getStatus() != ParticipantStatus.PENDING) {
            throw new AppException(ErrorStatus.FORBIDDEN, "This invitation has already been responded to");
        }

        if (status != ParticipantStatus.ACCEPTED && status != ParticipantStatus.DECLINED) {
            throw new AppException(ErrorStatus.INVALID_INPUT, "Status must be ACCEPTED or DECLINED");
        }

        participant.setStatus(status);
        participant.setRespondedAt(LocalDateTime.now());
        participantRepository.save(participant);
    }

    public List<Meeting> getPendingMeetings() {
        return meetingRepository.findByStatus(MeetingStatus.PENDING_APPROVAL);
    }

    @Transactional
    public Meeting approveMeeting(String id) {
        Meeting meeting = meetingRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorStatus.MEETING_NOT_FOUND));

        meeting.setStatus(MeetingStatus.SCHEDULED);
        Meeting savedMeeting = meetingRepository.save(meeting);

        UUID creatorId = savedMeeting.getCreator().getId();
        try {
            if (googleCalendarService.isConnected(creatorId)) {
                // If you already have syncMeetingToGoogle, use it:
                googleCalendarService.syncMeetingToGoogle(savedMeeting, creatorId);

                // Alternatively (if you want direct create/update):
                // String token = savedMeeting.getCreator().getAccessToken();
                // if (token != null && !token.isEmpty()) {
                // if (savedMeeting.getGoogleEventId() == null) {
                // String eventId = googleCalendarService.createGoogleEvent(savedMeeting,
                // token);
                // savedMeeting.setGoogleEventId(eventId);
                // meetingRepository.save(savedMeeting);
                // } else {
                // googleCalendarService.updateGoogleEvent(savedMeeting.getGoogleEventId(),
                // savedMeeting, token);
                // }
                // }
            } else {
                log.info("Creator {} has not connected Google Calendar. Skipping sync.", creatorId);
            }
        } catch (Exception e) {
            log.warn("Failed to sync meeting {} to Google Calendar after approval", savedMeeting.getId(), e);
        }

        // Lấy tất cả cuộc họp PENDING_APPROVAL trong cùng phòng, cùng thời gian
        List<Meeting> conflictingPendingMeetings = meetingRepository
                .findConflictingPendingMeetings(
                        meeting.getMeetingRoom().getId(),
                        meeting.getStartTime(),
                        meeting.getEndTime(),
                        id);

        // Hủy tất cả các cuộc họp khác
        conflictingPendingMeetings.forEach(conflictMeeting -> {
            conflictMeeting.setStatus(MeetingStatus.CANCELLED);
            conflictMeeting.setCancelledAt(LocalDateTime.now());
            conflictMeeting.setCancellationReason("Cancelled due to conflicting approved meeting");
            meetingRepository.save(conflictMeeting);

            // Gửi email thông báo hủy cho creator và participants
            emailService.sendCancelMeetingEmail(conflictMeeting.getCreator().getEmail(), conflictMeeting);
            for (MeetingParticipant participant : conflictMeeting.getParticipants()) {
                emailService.sendCancelMeetingEmail(participant.getUser().getEmail(), conflictMeeting);
            }
        });

        return savedMeeting;
    }

    @Transactional
    public Meeting rejectMeeting(String id) {
        Meeting meeting = meetingRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorStatus.MEETING_NOT_FOUND));

        meeting.setStatus(MeetingStatus.CANCELLED);
        return meetingRepository.save(meeting);
    }

    public Page<MeetingResponse> getAllMeetings(int page, int size, String sortBy, String direction) {
        Pageable pageable = PageRequest.of(page, size,
                direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending());

        Page<Meeting> meetings = meetingRepository.findAll(pageable);
        return meetings.map(meetingMapper::toMeetingResponse);
    }

    public boolean isRoomAvailable(String roomId, String date, String start, String end) {
        DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

        String startIso = date + "T" + start; // ví dụ: "2025-12-09T07:36"
        String endIso = date + "T" + end;

        LocalDateTime startDateTime = LocalDateTime.parse(startIso, f);
        LocalDateTime endDateTime = LocalDateTime.parse(endIso, f);

        return !meetingRepository.existsConflict(roomId, startDateTime, endDateTime);
    }
}