package com.meeting.schedule_a_meeting.service.users;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
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

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final LocalTime MORNING_START = LocalTime.of(7, 0);
    private static final LocalTime MORNING_END = LocalTime.of(12, 0);
    private static final LocalTime AFTERNOON_START = LocalTime.of(13, 0);
    private static final LocalTime EVENING_END = LocalTime.of(23, 0);

    /* ====================== CREATE MEETING ====================== */
    @Transactional
    public MeetingResponse createMeeting(CreateMeetingRequest request, UUID creatorId) {
        log.info("=== CREATE MEETING REQUEST ===");
        log.info("Title: {}", request.getTitle());
        log.info("Date: {}", request.getDate());
        log.info("StartTime: {}, EndTime: {}", request.getStartTime(), request.getEndTime());
        log.info("IsRepeat: {}, RepeatDays: {}", request.isRepeat(), request.getRepeatDays());
        log.info("RoomId: {}", request.getRoomId());

        // Validate input
        if (request.getDate() == null || request.getDate().isEmpty()) {
            throw new AppException(ErrorStatus.INVALID_INPUT, "Date is required");
        }
        if (request.getStartTime() == null || request.getStartTime().isEmpty()) {
            throw new AppException(ErrorStatus.INVALID_INPUT, "Start time is required");
        }
        if (request.getEndTime() == null || request.getEndTime().isEmpty()) {
            throw new AppException(ErrorStatus.INVALID_INPUT, "End time is required");
        }

        // Parse time từ HH:mm format
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        LocalTime startTime = LocalTime.parse(request.getStartTime(), timeFormatter);
        LocalTime endTime = LocalTime.parse(request.getEndTime(), timeFormatter);

        // Validate meeting time
        validateMeetingTime(startTime, endTime);

        Users creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new AppException(ErrorStatus.USER_NOT_FOUND));

        MeetingRoom room = meetingRoomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new AppException(ErrorStatus.ROOM_NOT_FOUND));

        List<Meeting> createdMeetings = new ArrayList<>();

        log.info("About to create meetings - isRepeat: {}, repeatDays count: {}",
                request.isRepeat(), request.getRepeatDays() != null ? request.getRepeatDays().size() : 0);

        if (request.isRepeat() && request.getRepeatDays() != null && !request.getRepeatDays().isEmpty()) {
            // Tạo repeat meetings (1 cho mỗi ngày)
            log.info("Creating repeat meetings for days: {}", request.getRepeatDays());
            createdMeetings = createRepeatMeetings(request, creator, room, startTime, endTime);
        } else {
            // Tạo single meeting
            log.info("Creating single meeting (isRepeat={}, repeatDays={})",
                    request.isRepeat(), request.getRepeatDays());
            Meeting meeting = createSingleMeeting(request, creator, room, startTime, endTime);
            createdMeetings.add(meeting);
        }

        log.info("Total meetings created: {}", createdMeetings.size());

        // Fetch first meeting để return
        Meeting firstMeeting = meetingRepository.findById(createdMeetings.get(0).getId())
                .orElseThrow(() -> new AppException(ErrorStatus.MEETING_NOT_FOUND));

        return meetingMapper.toMeetingResponse(firstMeeting);
    }

    /**
     * Tạo meetings lặp lại cho từng ngày được chọn trong tuần
     */
    private List<Meeting> createRepeatMeetings(CreateMeetingRequest request, Users creator,
            MeetingRoom room, LocalTime startTime, LocalTime endTime) {

        String repeatGroupId = UUID.randomUUID().toString();
        List<Meeting> meetings = new ArrayList<>();
        LocalDate today = LocalDate.now();

        // Parse date từ request (yyyy-MM-dd)
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate selectedDate = LocalDate.parse(request.getDate(), dateFormatter);

        // Lấy Monday của tuần chứa selectedDate
        LocalDate monday = selectedDate.with(DayOfWeek.MONDAY);
        log.info("Selected date: {}, Monday of that week: {}", selectedDate, monday);

        String repeatDaysJson = convertListToJson(request.getRepeatDays());

        for (String dayStr : request.getRepeatDays()) {
            DayOfWeek dayOfWeek = DayOfWeek.valueOf(dayStr);
            // Tính ngày trong tuần được chọn
            LocalDate meetingDate = monday.with(dayOfWeek);

            log.info("Processing day: {} -> date: {}", dayStr, meetingDate);

            // Kiểm tra ngày không được trong quá khứ
            if (meetingDate.isBefore(today)) {
                log.warn("Day {} ({}) is in the past", dayStr, meetingDate);
                throw new AppException(ErrorStatus.MEETING_INVALID_TIME_RANGE,
                        dayStr + " has already passed this week");
            }

            // Tạo datetime từ LocalDate và LocalTime
            LocalDateTime startDateTime = LocalDateTime.of(meetingDate, startTime);
            LocalDateTime endDateTime = LocalDateTime.of(meetingDate, endTime);

            // Kiểm tra room availability
            validateRoomAvailability(room.getId(), startDateTime, endDateTime, null);

            // Build meeting
            Meeting meeting = Meeting.builder()
                    .title(request.getTitle())
                    .description(request.getDescription())
                    .startTime(startDateTime)
                    .endTime(endDateTime)
                    .meetingRoom(room)
                    .creator(creator)
                    .status(MeetingStatus.PENDING_APPROVAL)
                    .createdBy(creator.getName())
                    .isRepeat(true)
                    .repeatGroupId(repeatGroupId)
                    .repeatDays(repeatDaysJson)
                    .build();

            meetingRepository.saveAndFlush(meeting);
            log.info("Meeting created: {} for date {}", meeting.getId(), meetingDate);

            // Assign devices và participants
            assignDefaultRoomDevices(meeting);
            addParticipantsByEmail(meeting, request.getParticipants(), creator.getId());

            if (request.getBorrowedDevices() != null && !request.getBorrowedDevices().isEmpty()) {
                request.getBorrowedDevices().forEach(deviceReq -> borrowAdditionalDevice(meeting, deviceReq));
            }

            meetings.add(meeting);
        }

        return meetings;
    }

    /**
     * Tạo single meeting cho ngày được chọn
     */
    private Meeting createSingleMeeting(CreateMeetingRequest request, Users creator,
            MeetingRoom room, LocalTime startTime, LocalTime endTime) {

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate selectedDate = LocalDate.parse(request.getDate(), dateFormatter);

        LocalDateTime startDateTime = LocalDateTime.of(selectedDate, startTime);
        LocalDateTime endDateTime = LocalDateTime.of(selectedDate, endTime);

        log.info("Creating single meeting for date: {}", selectedDate);

        // Kiểm tra room availability
        validateRoomAvailability(room.getId(), startDateTime, endDateTime, null);

        Meeting meeting = Meeting.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .startTime(startDateTime)
                .endTime(endDateTime)
                .meetingRoom(room)
                .creator(creator)
                .status(MeetingStatus.PENDING_APPROVAL)
                .createdBy(creator.getName())
                .isRepeat(false)
                .build();

        meetingRepository.saveAndFlush(meeting);
        log.info("Single meeting created: {}", meeting.getId());

        assignDefaultRoomDevices(meeting);
        addParticipantsByEmail(meeting, request.getParticipants(), creator.getId());

        if (request.getBorrowedDevices() != null && !request.getBorrowedDevices().isEmpty()) {
            request.getBorrowedDevices().forEach(deviceReq -> borrowAdditionalDevice(meeting, deviceReq));
        }

        return meeting;
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
        if (meeting.getEndTime() != null && meeting.getEndTime().isBefore(LocalDateTime.now())) {
            throw new AppException(ErrorStatus.MEETING_CANNOT_EDIT_PAST, "This meeting has concluded");
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
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
            LocalTime startTime = LocalTime.parse(request.getStartTime(), timeFormatter);
            LocalTime endTime = LocalTime.parse(request.getEndTime(), timeFormatter);
            validateMeetingTime(startTime, endTime);

            LocalDate meetingDate = meeting.getStartTime().toLocalDate();
            LocalDateTime startDateTime = LocalDateTime.of(meetingDate, startTime);
            LocalDateTime endDateTime = LocalDateTime.of(meetingDate, endTime);

            validateRoomAvailability(
                    request.getRoomId() != null ? request.getRoomId() : meeting.getMeetingRoom().getId(),
                    startDateTime, endDateTime, meetingId);
            meeting.setStartTime(startDateTime);
            meeting.setEndTime(endDateTime);
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

        // Sync với Google Calendar nếu meeting đã được approved
        if (saved.getStatus() == MeetingStatus.SCHEDULED && googleCalendarService.isConnected(userId)) {
            try {
                googleCalendarService.syncMeetingToGoogle(saved, userId);
            } catch (Exception e) {
                log.warn("Failed to sync meeting update to Google Calendar", e);
            }
        }

        // Gửi email thông báo update
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

        return meetingMapper.toMeetingResponse(saved);
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
                meeting.setGoogleEventId(null);
                meeting.setLastSyncedAt(null);
                meetingRepository.save(meeting);
                log.info("Successfully deleted Google Calendar event for cancelled meeting {}", meetingId);
            } catch (Exception e) {
                log.warn("Failed to delete Google Calendar event", e);
            }
        }

        returnBorrowedDevices(meeting);

        emailService.sendCancelMeetingEmail(meeting.getCreator().getEmail(), meeting);
        for (MeetingParticipant participant : meeting.getParticipants()) {
            emailService.sendCancelMeetingEmail(participant.getUser().getEmail(), meeting);
        }
    }

    /* ====================== APPROVE MEETING ====================== */
    @Transactional
    public Meeting approveMeeting(String id) {
        Meeting meeting = meetingRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorStatus.MEETING_NOT_FOUND));

        if (meeting.getStatus() == MeetingStatus.CANCELLED) {
            throw new AppException(ErrorStatus.MEETING_ALREADY_CANCELLED);
        }

        meeting.setStatus(MeetingStatus.SCHEDULED);
        Meeting savedMeeting = meetingRepository.save(meeting);
        log.info("Meeting {} approved", id);

        UUID creatorId = savedMeeting.getCreator().getId();

        // Sync với Google Calendar
        try {
            if (googleCalendarService.isConnected(creatorId)) {
                googleCalendarService.syncMeetingToGoogle(savedMeeting, creatorId);
            } else {
                log.info("Creator {} has not connected Google Calendar", creatorId);
            }
        } catch (Exception e) {
            log.warn("Failed to sync meeting {} to Google Calendar", savedMeeting.getId(), e);
        }

        // Nếu là repeat meeting, approve tất cả meetings trong group
        if (savedMeeting.isRepeat() && savedMeeting.getRepeatGroupId() != null) {
            log.info("Approving all repeat meetings with group ID: {}", savedMeeting.getRepeatGroupId());
            List<Meeting> groupMeetings = meetingRepository.findByRepeatGroupId(savedMeeting.getRepeatGroupId());

            for (Meeting m : groupMeetings) {
                if (!m.getId().equals(id) && m.getStatus() == MeetingStatus.PENDING_APPROVAL) {
                    m.setStatus(MeetingStatus.SCHEDULED);
                    meetingRepository.save(m);
                    log.info("Meeting {} approved as part of repeat group", m.getId());

                    // Sync to Google Calendar for this meeting too
                    try {
                        if (googleCalendarService.isConnected(creatorId)) {
                            googleCalendarService.syncMeetingToGoogle(m, creatorId);
                        }
                    } catch (Exception e) {
                        log.warn("Failed to sync repeat meeting {} to Google Calendar", m.getId(), e);
                    }
                }
            }
        }

        return savedMeeting;
    }

    /* ====================== REJECT MEETING ====================== */
    @Transactional
    public Meeting rejectMeeting(String id) {
        Meeting meeting = meetingRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorStatus.MEETING_NOT_FOUND));

        if (meeting.getStatus() == MeetingStatus.CANCELLED) {
            throw new AppException(ErrorStatus.MEETING_ALREADY_CANCELLED);
        }

        meeting.setStatus(MeetingStatus.CANCELLED);
        meeting.setCancelledAt(LocalDateTime.now());
        meeting.setCancellationReason("Rejected by admin");
        Meeting savedMeeting = meetingRepository.save(meeting);
        log.info("Meeting {} rejected", id);

        // Nếu là repeat meeting, reject tất cả meetings trong group
        if (savedMeeting.isRepeat() && savedMeeting.getRepeatGroupId() != null) {
            log.info("Rejecting all repeat meetings with group ID: {}", savedMeeting.getRepeatGroupId());
            List<Meeting> groupMeetings = meetingRepository.findByRepeatGroupId(savedMeeting.getRepeatGroupId());

            for (Meeting m : groupMeetings) {
                if (!m.getId().equals(id) && m.getStatus() == MeetingStatus.PENDING_APPROVAL) {
                    m.setStatus(MeetingStatus.CANCELLED);
                    m.setCancelledAt(LocalDateTime.now());
                    m.setCancellationReason("Rejected as part of repeat group");
                    meetingRepository.save(m);
                    log.info("Meeting {} rejected as part of repeat group", m.getId());
                }
            }
        }

        return savedMeeting;
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

    public Page<MeetingResponse> getAllMeetings(int page, int size, String sortBy, String direction) {
        Pageable pageable = PageRequest.of(page, size,
                direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending());

        Page<Meeting> meetings = meetingRepository.findAll(pageable);
        return meetings.map(meetingMapper::toMeetingResponse);
    }

    public List<Meeting> getPendingMeetings() {
        return meetingRepository.findByStatus(MeetingStatus.PENDING_APPROVAL);
    }

    public boolean isRoomAvailable(String roomId, String date, String start, String end) {
        DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

        String startIso = date + "T" + start;
        String endIso = date + "T" + end;

        LocalDateTime startDateTime = LocalDateTime.parse(startIso, f);
        LocalDateTime endDateTime = LocalDateTime.parse(endIso, f);

        return !meetingRepository.existsConflict(roomId, startDateTime, endDateTime);
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

    /* ====================== PRIVATE HELPERS ====================== */
    private void validateMeetingTime(LocalTime start, LocalTime end) {
        if (end.isBefore(start) || end.equals(start)) {
            throw new AppException(ErrorStatus.MEETING_END_BEFORE_START);
        }

        boolean morning = !start.isBefore(MORNING_START) && !end.isAfter(MORNING_END);
        boolean afternoon = !start.isBefore(AFTERNOON_START) && !end.isAfter(EVENING_END);

        if (!(morning || afternoon)) {
            throw new AppException(ErrorStatus.MEETING_INVALID_TIME_RANGE);
        }
    }

    private void validateRoomAvailability(String roomId, LocalDateTime start, LocalDateTime end, String excludeId) {
        List<Meeting> conflicts = meetingRepository.findConflictingMeetings(roomId, start, end);
        if (!conflicts.isEmpty()) {
            throw new AppException(ErrorStatus.MEETING_ROOM_NOT_AVAILABLE);
        }
    }

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
                    meeting.addDevice(md);
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

        if (device.getStatus() != DeviceStatus.ACTIVE) {
            throw new AppException(ErrorStatus.DEVICE_NOT_ACTIVE);
        }

        if (device.getAvailableQuantity() < req.getQuantity()) {
            throw new AppException(ErrorStatus.INSUFFICIENT_QUANTITY);
        }

        device.setAvailableQuantity(device.getAvailableQuantity() - req.getQuantity());
        deviceRepository.save(device);

        MeetingDevice md = MeetingDevice.builder()
                .meeting(meeting)
                .device(device)
                .quantity(req.getQuantity())
                .status(MeetingDeviceStatus.RESERVED)
                .notes(req.getNotes())
                .build();
        meetingDeviceRepository.save(md);
        meeting.addDevice(md);
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

    private void updateParticipants(Meeting meeting, List<ParticipantRequest> requests, UUID creatorId) {
        participantRepository.deleteByMeetingId(meeting.getId());
        meeting.getParticipants().clear();

        if (requests != null) {
            String creatorEmail = meeting.getCreator().getEmail();
            requests = requests.stream()
                    .filter(pr -> pr.getEmail() != null &&
                            !pr.getEmail().equalsIgnoreCase(creatorEmail))
                    .toList();
        }

        addParticipantsByEmail(meeting, requests, creatorId);
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

    private boolean hasAccess(Meeting meeting, UUID userId) {
        return meeting.getCreator().getId().equals(userId) ||
                participantRepository.existsByMeetingIdAndUserId(meeting.getId(), userId);
    }

    private String convertListToJson(List<String> list) {
        try {
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            log.error("Error converting list to JSON", e);
            return "[]";
        }
    }

    @Transactional(readOnly = true)
    public List<MeetingResponse> getScheduledActiveMeetingsInRoom(String roomId, UUID currentUserId) {
        List<Meeting> meetings = meetingRepository.findScheduledActiveMeetingsInRoom(roomId);

        return meetings.stream()
                .map(meeting -> meetingMapper.toMeetingResponse(meeting))
                .collect(Collectors.toList());
    }

}