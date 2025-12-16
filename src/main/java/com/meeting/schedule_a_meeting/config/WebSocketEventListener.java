package com.meeting.schedule_a_meeting.config;

import com.meeting.schedule_a_meeting.dto.UserStatusDto;
import com.meeting.schedule_a_meeting.entities.UserStatus;
import com.meeting.schedule_a_meeting.enums.UserStatusEnum;

import com.meeting.schedule_a_meeting.repositories.chat_call.UserStatusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {

    private final UserStatusRepository userStatusRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String userId = headerAccessor.getNativeHeader("userId") != null
                ? headerAccessor.getNativeHeader("userId").get(0)
                : null;

        if (userId != null) {
            try {
                UUID userUUID = UUID.fromString(userId);
                UserStatus status = userStatusRepository.findByUserId(userUUID)
                        .orElse(UserStatus.builder()
                                .userId(userUUID)
                                .status(UserStatusEnum.ONLINE)
                                .sessionId(sessionId)
                                .lastSeenAt(LocalDateTime.now())
                                .build());

                status.setStatus(UserStatusEnum.ONLINE);
                status.setSessionId(sessionId);
                status.setLastSeenAt(LocalDateTime.now());
                userStatusRepository.save(status);

                log.info("User {} connected with session {}", userId, sessionId);

                // Broadcast user online status
                messagingTemplate.convertAndSend("/topic/user-status/" + userId,
                        UserStatusDto.builder()
                                .userId(userUUID)
                                .status("ONLINE")
                                .build());

            } catch (Exception e) {
                log.error("Error handling WebSocket connect", e);
            }
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String userId = (String) headerAccessor.getHeader("simpUser");

        if (userId != null) {
            try {
                UUID userUUID = UUID.fromString(userId);
                UserStatus status = userStatusRepository.findByUserId(userUUID).orElse(null);

                if (status != null) {
                    status.setStatus(UserStatusEnum.OFFLINE);
                    status.setLastSeenAt(LocalDateTime.now());
                    userStatusRepository.save(status);

                    log.info("User {} disconnected from session {}", userId, sessionId);

                    // Broadcast user offline status
                    messagingTemplate.convertAndSend("/topic/user-status/" + userId,
                            UserStatusDto.builder()
                                    .userId(userUUID)
                                    .status("OFFLINE")
                                    .build());
                }
            } catch (Exception e) {
                log.error("Error handling WebSocket disconnect", e);
            }
        }
    }
}
