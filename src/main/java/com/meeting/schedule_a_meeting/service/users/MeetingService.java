package com.meeting.schedule_a_meeting.service.users;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import com.meeting.schedule_a_meeting.dto.response.admin.PendingDeviceRequestDto;
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
import com.meeting.schedule_a_meeting.entities.RoomDevice;
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
        log.info("=== CREATE MEETING REQUEST (WITH ACCURATE REPEAT SUPPORT) ===");
        log.info("Title: {}", request.getTitle());
        log.info("Date: {}", request.getDate());
        log.info("StartTime: {}, EndTime: {}", request.getStartTime(), request.getEndTime());
        log.info("RoomId: {}", request.getRoomId());
        log.info("RepeatType: {}", request.getRepeatType());
        log.info("RepeatUntilDate: {}", request.getRepeatUntilDate());
        log.info("RepeatWeeks: {}", request.getRepeatWeeks());
        log.info("RepeatDays: {}", request.getRepeatDays());

        // Validation cơ bản
        if (request.getDate() == null || request.getDate().isEmpty()) {
            throw new AppException(ErrorStatus.INVALID_INPUT, "Date is required");
        }
        if (request.getStartTime() == null || request.getStartTime().isEmpty()) {
            throw new AppException(ErrorStatus.INVALID_INPUT, "Start time is required");
        }
        if (request.getEndTime() == null || request.getEndTime().isEmpty()) {
            throw new AppException(ErrorStatus.INVALID_INPUT, "End time is required");
        }

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        LocalTime startTime = LocalTime.parse(request.getStartTime(), timeFormatter);
        LocalTime endTime = LocalTime.parse(request.getEndTime(), timeFormatter);
        validateMeetingTime(startTime, endTime);

        Users creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new AppException(ErrorStatus.USER_NOT_FOUND));

        MeetingRoom room = meetingRoomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new AppException(ErrorStatus.ROOM_NOT_FOUND));

        LocalDate baseDate = LocalDate.parse(request.getDate());
        LocalDateTime baseStart = LocalDateTime.of(baseDate, startTime);
        LocalDateTime baseEnd = LocalDateTime.of(baseDate, endTime);

        // Xử lý repeat
        String repeatTypeStr = request.getRepeatType();
        boolean isRepeat = false;
        String repeatGroupId = null;
        LocalDate seriesEndDate = null;
        List<String> daysToRepeat = null;

        if (repeatTypeStr != null && !repeatTypeStr.isEmpty()) {
            String normalizedType = repeatTypeStr.toUpperCase();
            isRepeat = Set.of("DAILY", "WEEKLY", "CUSTOM").contains(normalizedType);

            if (isRepeat) {
                repeatGroupId = UUID.randomUUID().toString();

                // Ưu tiên: Nếu có repeatUntilDate → dùng chính xác ngày này làm giới hạn
                if (request.getRepeatUntilDate() != null && !request.getRepeatUntilDate().isEmpty()) {
                    try {
                        LocalDate untilDate = LocalDate.parse(request.getRepeatUntilDate());
                        if (untilDate.isBefore(baseDate)) {
                            throw new AppException(ErrorStatus.INVALID_INPUT,
                                    "Repeat until date must be on or after the start date");
                        }
                        seriesEndDate = untilDate; // inclusive

                        // Xác định daysToRepeat dựa trên loại repeat
                        switch (normalizedType) {
                            case "DAILY":
                                daysToRepeat = List.of("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY",
                                        "SATURDAY", "SUNDAY");
                                break;
                            case "WEEKLY":
                                daysToRepeat = List.of(baseDate.getDayOfWeek().name());
                                break;
                            case "CUSTOM":
                                if (request.getRepeatDays() == null || request.getRepeatDays().isEmpty()) {
                                    throw new AppException(ErrorStatus.INVALID_INPUT,
                                            "At least one day must be selected for custom repeat");
                                }
                                daysToRepeat = new ArrayList<>(request.getRepeatDays());
                                break;
                        }
                    } catch (DateTimeParseException e) {
                        throw new AppException(ErrorStatus.INVALID_INPUT,
                                "Invalid repeatUntilDate format. Use yyyy-MM-dd");
                    }
                }
                // Fallback: Không có repeatUntilDate → dùng logic cũ (theo tuần/tháng)
                else {
                    switch (normalizedType) {
                        case "DAILY":
                            Integer monthsDaily = request.getRepeatEndAfterMonths();
                            if (monthsDaily == null || monthsDaily < 1 || monthsDaily > 2) {
                                throw new AppException(ErrorStatus.INVALID_INPUT,
                                        "Daily repeat must end after 1 or 2 months");
                            }
                            seriesEndDate = baseDate.plusMonths(monthsDaily).minusDays(1);
                            daysToRepeat = List.of("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY",
                                    "SUNDAY");
                            break;

                        case "WEEKLY":
                            Integer weeks = request.getRepeatWeeks();
                            if (weeks == null || weeks < 1 || weeks > 36) {
                                throw new AppException(ErrorStatus.INVALID_INPUT,
                                        "Weekly repeat must be between 1 and 36 weeks");
                            }
                            seriesEndDate = baseDate.plusWeeks(weeks).minusDays(1);
                            daysToRepeat = List.of(baseDate.getDayOfWeek().name());
                            break;

                        case "CUSTOM":
                            if (request.getRepeatDays() == null || request.getRepeatDays().isEmpty()) {
                                throw new AppException(ErrorStatus.INVALID_INPUT,
                                        "At least one day must be selected for custom repeat");
                            }
                            Integer monthsCustom = request.getRepeatEndAfterMonths();
                            if (monthsCustom == null || monthsCustom < 1 || monthsCustom > 2) {
                                throw new AppException(ErrorStatus.INVALID_INPUT,
                                        "Custom repeat must end after 1 or 2 months");
                            }
                            seriesEndDate = baseDate.plusMonths(monthsCustom).minusDays(1);
                            daysToRepeat = new ArrayList<>(request.getRepeatDays());
                            break;
                    }
                }
            }
        }

        // Tạo danh sách các ngày cần tạo meeting
        List<LocalDate> targetDates = new ArrayList<>();

        targetDates.add(baseDate);

        if (isRepeat && seriesEndDate != null) {
            // Bắt đầu lặp từ ngày TIẾP THEO ngày gốc để tránh trùng
            LocalDate current = baseDate.plusDays(1);
            while (!current.isAfter(seriesEndDate)) {
                if (daysToRepeat.contains(current.getDayOfWeek().name())) {
                    targetDates.add(current);
                }
                current = current.plusDays(1);
            }
        }

        if (targetDates.isEmpty()) {
            throw new AppException(ErrorStatus.INVALID_INPUT, "No valid dates generated for the meeting series");
        }

        log.info("Generating {} meeting(s) on dates: {}", targetDates.size(), targetDates);

        List<Meeting> createdMeetings = new ArrayList<>();

        for (LocalDate date : targetDates) {
            LocalDateTime startDateTime = LocalDateTime.of(date, startTime);
            LocalDateTime endDateTime = LocalDateTime.of(date, endTime);

            // Kiểm tra xung đột phòng
            List<Meeting> conflicts = meetingRepository.findConflictingMeetings(
                    room.getId(), startDateTime, endDateTime);

            if (!conflicts.isEmpty()) {
                throw new AppException(ErrorStatus.MEETING_ROOM_NOT_AVAILABLE,
                        "Room not available on " + date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            }

            Meeting meeting = Meeting.builder()
                    .title(request.getTitle())
                    .description(request.getDescription())
                    .startTime(startDateTime)
                    .endTime(endDateTime)
                    .meetingRoom(room)
                    .creator(creator)
                    .createdBy(creator.getEmail())
                    .status(MeetingStatus.PENDING_APPROVAL)
                    .isRepeat(isRepeat)
                    .repeatGroupId(repeatGroupId)
                    .repeatDays(daysToRepeat != null ? String.join(",", daysToRepeat) : null)
                    .build();

            meetingRepository.save(meeting);

            assignDefaultRoomDevices(meeting);
            addParticipantsByEmail(meeting, request.getParticipants(), creatorId);

            if (request.getBorrowedDevices() != null && !request.getBorrowedDevices().isEmpty()) {
                for (DeviceBorrowRequest deviceReq : request.getBorrowedDevices()) {
                    borrowAdditionalDevice(meeting, deviceReq);
                }
            }

            createdMeetings.add(meeting);
        }

        log.info("Successfully created {} meeting(s) with groupId: {}", createdMeetings.size(), repeatGroupId);

        // Trả về meeting đầu tiên (ngày gốc)
        return meetingMapper.toMeetingResponse(createdMeetings.get(0));
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
            // Parse trực tiếp ISO_LOCAL_DATE_TIME (ví dụ: 2025-12-15T14:00:00)
            LocalDateTime startDateTime = LocalDateTime.parse(request.getStartTime(),
                    DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            LocalDateTime endDateTime = LocalDateTime.parse(request.getEndTime(),
                    DateTimeFormatter.ISO_LOCAL_DATE_TIME);

            // Validate thời gian (chỉ lấy phần giờ phút để kiểm tra logic buổi sáng/chiều)
            validateMeetingTime(startDateTime.toLocalTime(), endDateTime.toLocalTime());

            // Kiểm tra room availability
            validateRoomAvailability(
                    request.getRoomId() != null ? request.getRoomId() : meeting.getMeetingRoom().getId(),
                    startDateTime, endDateTime, meetingId);

            // Gán lại cho meeting
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

        if (meeting.getRepeatGroupId() != null) {
            log.info("Approving entire repeat series - groupId: {}", meeting.getRepeatGroupId());
            List<Meeting> series = meetingRepository.findByRepeatGroupId(meeting.getRepeatGroupId());

            for (Meeting m : series) {
                if (m.getStatus() == MeetingStatus.PENDING_APPROVAL) {
                    m.setStatus(MeetingStatus.SCHEDULED);
                }
            }
            meetingRepository.saveAll(series);

            // Trả về meeting đầu tiên trong chuỗi
            return series.stream()
                    .min(Comparator.comparing(Meeting::getStartTime))
                    .orElse(meeting);
        } else {
            meeting.setStatus(MeetingStatus.SCHEDULED);
            return meetingRepository.save(meeting);
        }
    }

    /* ====================== REJECT MEETING ====================== */
    @Transactional
    public Meeting rejectMeeting(String id) {
        Meeting meeting = meetingRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorStatus.MEETING_NOT_FOUND));

        if (meeting.getRepeatGroupId() != null) {
            log.info("Rejecting entire repeat series - groupId: {}", meeting.getRepeatGroupId());
            List<Meeting> series = meetingRepository.findByRepeatGroupId(meeting.getRepeatGroupId());

            for (Meeting m : series) {
                if (m.getStatus() == MeetingStatus.PENDING_APPROVAL) {
                    m.setStatus(MeetingStatus.CANCELLED);
                    m.setCancelledAt(LocalDateTime.now());
                    m.setCancellationReason("Rejected by admin");
                }
            }
            meetingRepository.saveAll(series);

            return series.stream()
                    .min(Comparator.comparing(Meeting::getStartTime))
                    .orElse(meeting);
        } else {
            meeting.setStatus(MeetingStatus.CANCELLED);
            meeting.setCancelledAt(LocalDateTime.now());
            meeting.setCancellationReason("Rejected by admin");
            return meetingRepository.save(meeting);
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

    // MeetingService.java
    public List<PendingDeviceRequestDto> getFrequentlyBorrowedDevices(String roomId) {
        List<MeetingDevice> borrowed = meetingDeviceRepository.findBorrowedAdditionalInRoom(roomId);

        if (borrowed.isEmpty()) {
            return new ArrayList<>();
        }

        Map<Device, Integer> totalByDevice = borrowed.stream()
                .collect(Collectors.groupingBy(
                        MeetingDevice::getDevice,
                        Collectors.summingInt(MeetingDevice::getQuantity)));

        Map<Device, Long> countByDevice = borrowed.stream()
                .collect(Collectors.groupingBy(
                        MeetingDevice::getDevice,
                        Collectors.counting()));

        MeetingRoom room = meetingRoomRepository.findById(roomId)
                .orElseThrow(() -> new AppException(ErrorStatus.ROOM_NOT_FOUND));

        return totalByDevice.entrySet().stream()
                .map(entry -> PendingDeviceRequestDto.builder()
                        .deviceId(entry.getKey().getId())
                        .deviceName(entry.getKey().getName())
                        .totalRequestedQuantity(entry.getValue())
                        .requestCount(countByDevice.getOrDefault(entry.getKey(), 0L).intValue())
                        .roomId(roomId)
                        .roomName(room.getName())
                        .build())
                .sorted(Comparator.comparingInt(PendingDeviceRequestDto::getTotalRequestedQuantity).reversed())
                .collect(Collectors.toList());
    }

    // 2. Admin gán cố định thiết bị vào phòng
    @Transactional
    public void assignDeviceToRoomPermanently(String roomId, String deviceId, int quantity) {
        if (quantity <= 0) {
            throw new AppException(ErrorStatus.INVALID_INPUT, "Quantity must be greater than 0");
        }

        MeetingRoom room = meetingRoomRepository.findById(roomId)
                .orElseThrow(() -> new AppException(ErrorStatus.ROOM_NOT_FOUND));

        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new AppException(ErrorStatus.DEVICE_NOT_FOUND));

        List<RoomDevice> existingList = roomDeviceRepository
                .findByMeetingRoomIdAndDeviceId(roomId, deviceId);

        RoomDevice existing = existingList.isEmpty() ? null : existingList.get(0);

        if (existing != null) {
            // Đã có → tăng số lượng
            existing.setQuantity(existing.getQuantity() + quantity);
            roomDeviceRepository.save(existing);
            log.info("Increased RoomDevice quantity: {} + {} for device {} in room {}",
                    existing.getQuantity() - quantity, quantity, device.getName(), room.getName());
        } else {
            // Chưa có → tạo mới
            RoomDevice newAssignment = RoomDevice.builder()
                    .meetingRoom(room)
                    .device(device)
                    .quantity(quantity)
                    .status(RoomDeviceStatus.IN_USE)
                    .build();
            roomDeviceRepository.save(newAssignment);
            log.info("Created new RoomDevice: {} x {} in room {}", quantity, device.getName(), room.getName());
        }
    }

}