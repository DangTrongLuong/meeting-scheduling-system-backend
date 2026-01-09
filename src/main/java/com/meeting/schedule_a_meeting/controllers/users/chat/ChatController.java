package com.meeting.schedule_a_meeting.controllers.users.chat;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.meeting.schedule_a_meeting.dto.request.users.chat.ChatMessageRequest;
import com.meeting.schedule_a_meeting.dto.response.users.chat.ChatConversationResponse;
import com.meeting.schedule_a_meeting.dto.response.users.chat.ChatMessageResponse;
import com.meeting.schedule_a_meeting.dto.response.users.meeting.ApiResponse;
import com.meeting.schedule_a_meeting.dto.response.users.meeting.UserSummary;
import com.meeting.schedule_a_meeting.service.users.ChatService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('USER','ADMIN','SUPERADMIN')")
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/send")
    public ResponseEntity<ApiResponse<ChatMessageResponse>> sendMessage(
            @RequestBody ChatMessageRequest request,
            @RequestHeader("userId") UUID userId) {
        log.info("Send message request from user: {}", userId);

        ChatMessageResponse response = chatService.sendMessage(request, userId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Message sent successfully", response));
    }

    @GetMapping("/conversations")
    public ResponseEntity<ApiResponse<List<ChatConversationResponse>>> getChatPartners(
            @RequestHeader("userId") UUID userId) {
        log.info("Fetching chat partners for user: {}", userId);

        List<ChatConversationResponse> response = chatService.getChatPartners(userId);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/history/{otherUserId}")
    public ResponseEntity<ApiResponse<Page<ChatMessageResponse>>> getChatHistory(
            @PathVariable UUID otherUserId,
            @RequestHeader("userId") UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Fetching chat history between {} and {}", userId, otherUserId);

        Page<ChatMessageResponse> response = chatService.getChatHistory(userId, otherUserId, page, size);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/mark-as-read/{messageId}")
    public ResponseEntity<ApiResponse<String>> markAsRead(@PathVariable UUID messageId) {
        chatService.markAsRead(messageId);

        return ResponseEntity.ok(ApiResponse.success("Message marked as read"));
    }

    @PatchMapping("/mark-all-as-read/{senderId}")
    public ResponseEntity<ApiResponse<String>> markAllAsRead(
            @PathVariable UUID senderId,
            @RequestHeader("userId") UUID userId) {
        chatService.markAllAsReadFrom(userId, senderId);

        return ResponseEntity.ok(ApiResponse.success("All messages marked as read"));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(@RequestHeader("userId") UUID userId) {
        long count = chatService.getUnreadCount(userId);

        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    @GetMapping("/search-users")
    public ResponseEntity<ApiResponse<List<UserSummary>>> searchUsers(
            @RequestParam String keyword,
            @RequestHeader("userId") UUID userId) {
        log.info("Searching users with keyword: {}", keyword);

        List<UserSummary> response = chatService.searchUsers(keyword, userId);

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
