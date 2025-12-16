package com.meeting.schedule_a_meeting.dto.response.users.chat;

import java.time.LocalDateTime;
import java.util.UUID;

import com.meeting.schedule_a_meeting.enums.UserStatusEnum;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatConversationResponse {

    private UUID userId;
    private String name;
    private String avatar;
    private String lastMessage;
    private LocalDateTime lastMessageTime;
    private long unreadCount;
    private UserStatusEnum status;
    private LocalDateTime lastSeenAt;
}
