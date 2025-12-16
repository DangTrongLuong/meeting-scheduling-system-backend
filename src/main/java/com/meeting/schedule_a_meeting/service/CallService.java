package com.meeting.schedule_a_meeting.service;

import com.meeting.schedule_a_meeting.dto.request.users.call.InitiateCallRequest;
import com.meeting.schedule_a_meeting.dto.response.users.call.CallParticipantResponse;
import com.meeting.schedule_a_meeting.dto.response.users.call.CallSessionResponse;
import com.meeting.schedule_a_meeting.dto.response.users.call.TodayMeetingsResponse;
import com.meeting.schedule_a_meeting.entities.*;
import com.meeting.schedule_a_meeting.enums.CallParticipantStatus;
import com.meeting.schedule_a_meeting.enums.CallStatus;
import com.meeting.schedule_a_meeting.enums.ErrorStatus;
import com.meeting.schedule_a_meeting.enums.MeetingStatus;
import com.meeting.schedule_a_meeting.exception.AppException;
import com.meeting.schedule_a_meeting.repositories.UserRepository;
import com.meeting.schedule_a_meeting.repositories.chat_call.CallParticipantRepository;
import com.meeting.schedule_a_meeting.repositories.chat_call.CallSessionRepository;
import com.meeting.schedule_a_meeting.repositories.meeting.MeetingParticipantRepository;
import com.meeting.schedule_a_meeting.repositories.meeting.MeetingRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CallService {

    private final CallSessionRepository callSessionRepository;
    private final CallParticipantRepository callParticipantRepository;
    private final MeetingRepository meetingRepository;
    private final MeetingParticipantRepository meetingParticipantRepository;
    private final UserRepository userRepository;

    /**
     * Khởi tạo cuộc gọi cho một meeting
     */
    @Transactional
    public CallSessionResponse initiateCall(InitiateCallRequest request, UUID initiatorId) {
        log.info("Initiating call for meeting: {} by user: {}", request.getMeetingId(), initiatorId);

        Meeting meeting = meetingRepository.findById(request.getMeetingId())
                .orElseThrow(() -> new AppException(ErrorStatus.MEETING_NOT_FOUND));

        // Kiểm tra meeting có đang trong ngày hôm nay không
        LocalDate today = LocalDate.now();
        if (!meeting.getStartTime().toLocalDate().equals(today)) {
            throw new AppException(ErrorStatus.INVALID_INPUT, "Can only initiate calls for today's meetings");
        }

        // Kiểm tra thời gian hiện tại có gần thời gian bắt đầu meeting không (cho phép 15 phút trước)
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(meeting.getStartTime().minusMinutes(15))) {
            throw new AppException(ErrorStatus.INVALID_INPUT, "Call can only be initiated 15 minutes before meeting start time");
        }

        Users initiator = userRepository.findById(initiatorId)
                .orElseThrow(() -> new AppException(ErrorStatus.USER_NOT_FOUND));

        // Kiểm tra có call đang active không
        var existingCall = callSessionRepository.findActiveCallByMeeting(request.getMeetingId());
        if (existingCall.isPresent()) {
            throw new AppException(ErrorStatus.INVALID_INPUT, "Call already initiated for this meeting");
        }

        // Tạo call session
        CallSession callSession = CallSession.builder()
                .meeting(meeting)
                .initiator(initiator)
                .status(CallStatus.RINGING)
                .build();

        CallSession savedCall = callSessionRepository.save(callSession);
        log.info("Call session created: {}", savedCall.getId());

        // Thêm tất cả participants của meeting vào call
        List<MeetingParticipant> meetingParticipants = meetingParticipantRepository.findByMeetingId(meeting.getId());

        for (MeetingParticipant mp : meetingParticipants) {
            CallParticipant cp = CallParticipant.builder()
                    .callSession(savedCall)
                    .user(mp.getUser())
                    .status(CallParticipantStatus.RINGING)
                    .isMuted(false)
                    .isVideoEnabled(true)
                    .build();
            callParticipantRepository.save(cp);
            log.info("Added participant {} to call", mp.getUser().getId());
        }

        // Thêm initiator nếu là creator của meeting
        if (!meeting.getCreator().getId().equals(initiatorId)) {
            CallParticipant initiatorParticipant = CallParticipant.builder()
                    .callSession(savedCall)
                    .user(initiator)
                    .status(CallParticipantStatus.ACCEPTED)
                    .joinedAt(LocalDateTime.now())
                    .isMuted(false)
                    .isVideoEnabled(true)
                    .build();
            callParticipantRepository.save(initiatorParticipant);
        }

        return toCallSessionResponse(savedCall);
    }

    /**
     * Lấy cuộc gọi đang active của một meeting
     */
    @Transactional(readOnly = true)
    public CallSessionResponse getActiveCall(String meetingId) {
        var callSession = callSessionRepository.findActiveCallByMeeting(meetingId)
                .orElseThrow(() -> new AppException(ErrorStatus.CALL_ACTIVE_NOT_FOUND, "No active call for this meeting"));

        return toCallSessionResponse(callSession);
    }

    /**
     * User chấp nhận cuộc gọi
     */
    @Transactional
    public void acceptCall(UUID callId, UUID userId) {
        CallSession callSession = callSessionRepository.findById(callId)
                .orElseThrow(() -> new AppException(ErrorStatus.CALL_NOT_FOUND, "Call not found"));

        CallParticipant participant = callParticipantRepository.findByCallSessionIdAndUserId(callId, userId)
                .orElseThrow(() -> new AppException(ErrorStatus.PARTICIPANT_NOT_FOUND, "Participant not found in call"));

        participant.setStatus(CallParticipantStatus.ACCEPTED);
        participant.setJoinedAt(LocalDateTime.now());
        callParticipantRepository.save(participant);

        // Update call status nếu là cuộc gọi được accepted lần đầu
        if (callSession.getStatus() == CallStatus.RINGING) {
            callSession.setStatus(CallStatus.ANSWERED);
            callSessionRepository.save(callSession);
        }

        log.info("User {} accepted call {}", userId, callId);
    }

    /**
     * User từ chối cuộc gọi
     */
    @Transactional
    public void declineCall(UUID callId, UUID userId) {
        CallSession callSession = callSessionRepository.findById(callId)
                .orElseThrow(() -> new AppException(ErrorStatus.CALL_NOT_FOUND, "Call not found"));

        CallParticipant participant = callParticipantRepository.findByCallSessionIdAndUserId(callId, userId)
                .orElseThrow(() -> new AppException(ErrorStatus.PARTICIPANT_NOT_FOUND, "Participant not found in call"));

        participant.setStatus(CallParticipantStatus.REJECTED);
        callParticipantRepository.save(participant);

        log.info("User {} declined call {}", userId, callId);
    }

    /**
     * Kết thúc cuộc gọi
     */
    @Transactional
    public void endCall(UUID callId) {
        CallSession callSession = callSessionRepository.findById(callId)
                .orElseThrow(() -> new AppException(ErrorStatus.CALL_NOT_FOUND, "Call not found"));

        callSession.setStatus(CallStatus.ENDED);
        callSession.setEndedAt(LocalDateTime.now());

        if (callSession.getStartedAt() != null) {
            long durationSeconds = java.time.temporal.ChronoUnit.SECONDS
                    .between(callSession.getStartedAt(), LocalDateTime.now());
            callSession.setDurationSeconds(durationSeconds);
        }

        callSessionRepository.save(callSession);
        log.info("Call {} ended", callId);
    }

    /**
     * Lấy danh sách meetings hôm nay
     */
    @Transactional(readOnly = true)
    public List<TodayMeetingsResponse> getTodayMeetings(UUID userId) {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);

        // Lấy meetings mà user là participant hoặc creator
        List<Meeting> meetings = meetingRepository.findMeetingsByUser(userId);

        return meetings.stream()
                .filter(m -> m.getStartTime().toLocalDate().equals(today) &&
                        m.getStatus() == MeetingStatus.SCHEDULED)
                .map(meeting -> {
                    var activeCall = callSessionRepository.findActiveCallByMeeting(meeting.getId());

                    LocalDateTime now = LocalDateTime.now();
                    boolean canInitiate = now.isAfter(meeting.getStartTime().minusMinutes(15));

                    return TodayMeetingsResponse.builder()
                            .meetingId(meeting.getId())
                            .title(meeting.getTitle())
                            .description(meeting.getDescription())
                            .startTime(meeting.getStartTime())
                            .endTime(meeting.getEndTime())
                            .roomName(meeting.getMeetingRoom().getName())
                            .participantCount(meeting.getParticipants().size())
                            .isCallActive(activeCall.isPresent())
                            .canInitiateCall(canInitiate)
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * Toggle mute cho participant
     */
    @Transactional
    public void toggleMute(UUID callId, UUID userId, boolean mute) {
        CallParticipant participant = callParticipantRepository.findByCallSessionIdAndUserId(callId, userId)
                .orElseThrow(() -> new AppException(ErrorStatus.PARTICIPANT_NOT_FOUND, "Participant not found"));

        participant.setMuted(mute);
        callParticipantRepository.save(participant);
        log.info("User {} mute status set to: {}", userId, mute);
    }

    /**
     * Toggle video cho participant
     */
    @Transactional
    public void toggleVideo(UUID callId, UUID userId, boolean enable) {
        CallParticipant participant = callParticipantRepository.findByCallSessionIdAndUserId(callId, userId)
                .orElseThrow(() -> new AppException(ErrorStatus.PARTICIPANT_NOT_FOUND, "Participant not found"));

        participant.setVideoEnabled(enable);
        callParticipantRepository.save(participant);
        log.info("User {} video enabled: {}", userId, enable);
    }

    /**
     * Convert CallSession to Response
     */
    private CallSessionResponse toCallSessionResponse(CallSession callSession) {
        List<CallParticipantResponse> participants = callSession.getParticipants().stream()
                .map(this::toCallParticipantResponse)
                .collect(Collectors.toList());

        return CallSessionResponse.builder()
                .callId(callSession.getId())
                .meetingId(callSession.getMeeting().getId())
                .meetingTitle(callSession.getMeeting().getTitle())
                .initiatorId(callSession.getInitiator().getId())
                .initiatorName(callSession.getInitiator().getName())
                .status(callSession.getStatus())
                .startedAt(callSession.getStartedAt())
                .endedAt(callSession.getEndedAt())
                .durationSeconds(callSession.getDurationSeconds())
                .participants(participants)
                .build();
    }

    /**
     * Convert CallParticipant to Response
     */
    private CallParticipantResponse toCallParticipantResponse(CallParticipant participant) {
        return CallParticipantResponse.builder()
                .participantId(participant.getId())
                .userId(participant.getUser().getId())
                .userName(participant.getUser().getName())
                .userAvatar(participant.getUser().getAvatar_url())
                .status(participant.getStatus())
                .joinedAt(participant.getJoinedAt())
                .leftAt(participant.getLeftAt())
                .durationSeconds(participant.getDurationSeconds())
                .isMuted(participant.isMuted())
                .isVideoEnabled(participant.isVideoEnabled())
                .build();
    }
}
