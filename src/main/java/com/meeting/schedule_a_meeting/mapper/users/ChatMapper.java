package com.meeting.schedule_a_meeting.mapper.users;

import com.meeting.schedule_a_meeting.dto.response.users.chat.ChatMessageResponse;
import org.springframework.stereotype.Component;


import com.meeting.schedule_a_meeting.entities.ChatMessage;

@Component
public class ChatMapper {

    public ChatMessageResponse toChatMessageResponse(ChatMessage chatMessage) {
        if (chatMessage == null) {
            return null;
        }

        return ChatMessageResponse.builder()
                .id(chatMessage.getId())
                .senderId(chatMessage.getSender().getId())
                .senderName(chatMessage.getSender().getName())
                .senderAvatar(chatMessage.getSender().getAvatar_url())
                .recipientId(chatMessage.getRecipient().getId())
                .recipientName(chatMessage.getRecipient().getName())
                .content(chatMessage.getContent())
                .sentAt(chatMessage.getSentAt())
                .isRead(chatMessage.isRead())
                .readAt(chatMessage.getReadAt())
                .build();
    }
}
