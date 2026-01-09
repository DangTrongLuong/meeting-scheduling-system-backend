package com.meeting.schedule_a_meeting.controllers.users.call;

import java.util.UUID;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import com.meeting.schedule_a_meeting.dto.response.users.call.CallSessionResponse;
import com.meeting.schedule_a_meeting.service.users.CallService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Slf4j
public class CallWebSocketController {

    private final CallService callService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/call.incoming")
    public void broadcastIncomingCall(UUID callId) {
        log.info("Broadcasting incoming call: {}", callId);

        CallSessionResponse callSession = callService.getActiveCall(callId.toString());

        // Send to all participants
        callSession.getParticipants().forEach(participant -> {
            messagingTemplate.convertAndSendToUser(
                    participant.getUserId().toString(),
                    "/topic/call-incoming",
                    callSession);
        });
    }

    @MessageMapping("/call.status")
    public void notifyCallStatus(UUID callId) {
        log.info("Notifying call status change: {}", callId);
        // Broadcast call status to all participants
        messagingTemplate.convertAndSend(
                "/topic/call-status/" + callId,
                "status_updated");
    }

    @SubscribeMapping("/user/topic/call-incoming")
    public void subscribeIncomingCall() {
        log.info("User subscribed to incoming calls");
    }

    @SubscribeMapping("/topic/call-status/{callId}")
    public void subscribeCallStatus(@DestinationVariable UUID callId) {
        log.info("User subscribed to call status: {}", callId);
    }
}