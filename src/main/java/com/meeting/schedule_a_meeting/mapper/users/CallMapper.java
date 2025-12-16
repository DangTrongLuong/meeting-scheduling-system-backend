package com.meeting.schedule_a_meeting.mapper.users;

import java.util.List;
import java.util.stream.Collectors;

import com.meeting.schedule_a_meeting.dto.response.users.call.CallParticipantResponse;
import com.meeting.schedule_a_meeting.dto.response.users.call.CallSessionResponse;
import org.springframework.stereotype.Component;


import com.meeting.schedule_a_meeting.entities.CallParticipant;
import com.meeting.schedule_a_meeting.entities.CallSession;

@Component
public class CallMapper {

    public CallSessionResponse toCallSessionResponse(CallSession callSession) {
        if (callSession == null) {
            return null;
        }

        List<CallParticipantResponse> participants = callSession.getParticipants()
                .stream()
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

    public CallParticipantResponse toCallParticipantResponse(CallParticipant callParticipant) {
        if (callParticipant == null) {
            return null;
        }

        return CallParticipantResponse.builder()
                .participantId(callParticipant.getId())
                .userId(callParticipant.getUser().getId())
                .userName(callParticipant.getUser().getName())
                .userAvatar(callParticipant.getUser().getAvatar_url())
                .status(callParticipant.getStatus())
                .joinedAt(callParticipant.getJoinedAt())
                .leftAt(callParticipant.getLeftAt())
                .durationSeconds(callParticipant.getDurationSeconds())
                .isMuted(callParticipant.isMuted())
                .isVideoEnabled(callParticipant.isVideoEnabled())
                .build();
    }
}
