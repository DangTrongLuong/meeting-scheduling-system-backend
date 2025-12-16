package com.meeting.schedule_a_meeting.dto.response.users.chat;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageResponse {

    private UUID id;
    private UUID senderId;
    private String senderName;
    private String senderAvatar;
    private UUID recipientId;
    private String recipientName;
    private String content;
    private LocalDateTime sentAt;
    private boolean isRead;
    private LocalDateTime readAt;
}
