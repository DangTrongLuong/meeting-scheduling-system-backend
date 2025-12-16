package com.meeting.schedule_a_meeting.controllers.users.chat;

import java.util.UUID;

import com.meeting.schedule_a_meeting.dto.request.users.chat.ChatMessageRequest;
import com.meeting.schedule_a_meeting.dto.response.users.chat.ChatMessageResponse;
import com.meeting.schedule_a_meeting.service.users.ChatService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;



import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;


    @MessageMapping("/chat.send")
    public void sendMessage(ChatMessageRequest message) {
        log.info("WebSocket message received from {}", message.getRecipientId());

        UUID senderId = UUID.fromString(message.getRecipientId()); // Get from Principal
        ChatMessageResponse response = chatService.sendMessage(message, senderId);

        // Send to recipient
        messagingTemplate.convertAndSendToUser(
                message.getRecipientId(),
                "/topic/chat",
                response);

        // Send to sender
        messagingTemplate.convertAndSendToUser(
                senderId.toString(),
                "/topic/chat",
                response);
    }

    @SubscribeMapping("/user/chat")
    public void subscribeUserChat() {
        log.info("User subscribed to chat");
    }

    @SubscribeMapping("/topic/user-status/{userId}")
    public void subscribeUserStatus(@DestinationVariable UUID userId) {
        log.info("Subscribed to status updates for user: {}", userId);
    }
}
