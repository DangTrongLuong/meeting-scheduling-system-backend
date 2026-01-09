package com.meeting.schedule_a_meeting.controllers.users.chat;

import java.security.Principal;
import java.util.UUID;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.meeting.schedule_a_meeting.dto.request.users.chat.ChatMessageRequest;
import com.meeting.schedule_a_meeting.dto.response.users.chat.ChatMessageResponse;
import com.meeting.schedule_a_meeting.service.users.ChatService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('USER','ADMIN','SUPERADMIN')")
public class ChatWebSocketController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Nhận tin nhắn realtime từ client qua WebSocket
     */
    @MessageMapping("/chat.send")
    public void sendMessage(@Payload ChatMessageRequest request, Principal principal) {
        if (principal == null || principal.getName() == null) {
            log.warn("Unauthorized WebSocket message attempt: no principal");
            return;
        }

        UUID senderId = UUID.fromString(principal.getName());
        log.info("Realtime message from user {} to recipient {}", senderId, request.getRecipientId());

        try {
            ChatMessageResponse response = chatService.sendMessage(request, senderId);

            // Gửi tin nhắn đến người nhận
            messagingTemplate.convertAndSendToUser(
                    request.getRecipientId(),
                    "/topic/chat",
                    response);

            // Gửi lại cho chính người gửi (để hiển thị ngay lập tức ở client của họ)
            messagingTemplate.convertAndSendToUser(
                    senderId.toString(),
                    "/topic/chat",
                    response);

        } catch (Exception e) {
            log.error("Error processing realtime message from {} to {}", senderId, request.getRecipientId(), e);
        }
    }

    /**
     * Client subscribe vào đây để nhận tin nhắn riêng tư
     */
    @SubscribeMapping("/user/topic/chat")
    public void onSubscribePrivateChat() {
        log.info("User subscribed to private chat channel /user/topic/chat");
    }
}