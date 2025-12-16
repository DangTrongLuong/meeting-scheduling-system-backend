package com.meeting.schedule_a_meeting.dto.request.users.chat;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageRequest {

    @NotBlank(message = "Content is required")
    private String content;

    @NotBlank(message = "Recipient ID is required")
    private String recipientId;
}