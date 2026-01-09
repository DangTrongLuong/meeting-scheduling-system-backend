package com.meeting.schedule_a_meeting.config;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import com.meeting.schedule_a_meeting.entities.UserStatus;
import com.meeting.schedule_a_meeting.enums.UserStatusEnum;
import com.meeting.schedule_a_meeting.repositories.chat_call.UserStatusRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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

        log.info("🔗 WebSocket Connect Event - Session: {}", sessionId);

        // ==================== LẤY USERID TỪ HEADER ====================
        String userId = null;

        // Thử cách 1: Native headers
        Object userIdHeader = headerAccessor.getNativeHeader("userId");
        if (userIdHeader != null) {
            if (userIdHeader instanceof java.util.List) {
                java.util.List<?> list = (java.util.List<?>) userIdHeader;
                if (!list.isEmpty()) {
                    userId = list.get(0).toString();
                    log.info("✅ Got userId from native header: {}", userId);
                }
            }
        }

        // Thử cách 2: Session attributes
        if (userId == null && headerAccessor.getUser() != null) {
            userId = headerAccessor.getUser().getName();
            log.info("✅ Got userId from principal: {}", userId);
        }

        // Thử cách 3: ConnectHeaders
        if (userId == null) {
            Object connectHeader = headerAccessor.getHeader("userId");
            if (connectHeader != null) {
                userId = connectHeader.toString();
                log.info("✅ Got userId from connectHeaders: {}", userId);
            }
        }

        if (userId == null) {
            log.warn("❌ Could not extract userId from headers");
            log.debug("All headers: {}", headerAccessor.toMap());
            return;
        }

        try {
            UUID userUUID = UUID.fromString(userId);

            // Lấy hoặc tạo status entity
            UserStatus status = userStatusRepository.findByUserId(userUUID).orElse(
                    UserStatus.builder()
                            .userId(userUUID)
                            .status(UserStatusEnum.OFFLINE)
                            .sessionId(sessionId)
                            .lastSeenAt(LocalDateTime.now())
                            .build());

            // SET ONLINE
            status.setStatus(UserStatusEnum.ONLINE);
            status.setSessionId(sessionId);
            status.setLastSeenAt(LocalDateTime.now());
            UserStatus saved = userStatusRepository.save(status);

            log.info("✅ User {} ONLINE (session: {})", userUUID, sessionId);
            log.info("✅ Status saved: {}", saved);

            // BROADCAST STATUS UPDATE
            messagingTemplate.convertAndSend("/topic/user-status/" + userUUID,
                    UserStatusDto.builder()
                            .userId(userUUID)
                            .status("ONLINE")
                            .build());

            log.info("✅ Broadcasted ONLINE status for user: {}", userUUID);

        } catch (Exception e) {
            log.error("❌ Error handling WebSocket connect", e);
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();

        log.info("🔌 WebSocket Disconnect Event - Session: {}", sessionId);

        // Lấy userId (khó hơn vì disconnect event không có header)
        String userId = null;

        // Thử từ session attributes
        if (headerAccessor.getSessionAttributes() != null) {
            Object userObj = headerAccessor.getSessionAttributes().get("userId");
            if (userObj != null) {
                userId = userObj.toString();
            }
        }

        // Thử từ nativeheader
        if (userId == null) {
            Object userIdHeader = headerAccessor.getNativeHeader("userId");
            if (userIdHeader instanceof java.util.List) {
                java.util.List<?> list = (java.util.List<?>) userIdHeader;
                if (!list.isEmpty()) {
                    userId = list.get(0).toString();
                }
            }
        }

        if (userId == null) {
            log.warn("❌ Could not extract userId on disconnect");
            return;
        }

        try {
            UUID userUUID = UUID.fromString(userId);

            UserStatus status = userStatusRepository.findByUserId(userUUID).orElse(null);

            if (status != null) {
                // SET OFFLINE
                status.setStatus(UserStatusEnum.OFFLINE);
                status.setLastSeenAt(LocalDateTime.now());
                userStatusRepository.save(status);

                log.info("❌ User {} OFFLINE", userUUID);

                // BROADCAST OFFLINE
                messagingTemplate.convertAndSend("/topic/user-status/" + userUUID,
                        UserStatusDto.builder()
                                .userId(userUUID)
                                .status("OFFLINE")
                                .build());

                log.info("✅ Broadcasted OFFLINE status for user: {}", userUUID);
            }
        } catch (Exception e) {
            log.error("❌ Error handling WebSocket disconnect", e);
        }
    }

    @lombok.Data
    @lombok.Builder
    public static class UserStatusDto {
        private UUID userId;
        private String status;
    }
}