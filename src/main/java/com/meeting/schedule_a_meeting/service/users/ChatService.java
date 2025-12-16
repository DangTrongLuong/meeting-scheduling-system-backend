package com.meeting.schedule_a_meeting.service.users;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.meeting.schedule_a_meeting.dto.request.users.chat.ChatMessageRequest;
import com.meeting.schedule_a_meeting.dto.response.users.chat.ChatConversationResponse;
import com.meeting.schedule_a_meeting.dto.response.users.chat.ChatMessageResponse;
import com.meeting.schedule_a_meeting.entities.ChatMessage;
import com.meeting.schedule_a_meeting.entities.UserStatus;
import com.meeting.schedule_a_meeting.entities.Users;
import com.meeting.schedule_a_meeting.enums.ErrorStatus;
import com.meeting.schedule_a_meeting.exception.AppException;
import com.meeting.schedule_a_meeting.repositories.UserRepository;
import com.meeting.schedule_a_meeting.repositories.chat_call.ChatMessageRepository;
import com.meeting.schedule_a_meeting.repositories.chat_call.UserStatusRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;



import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final UserStatusRepository userStatusRepository;

    /**
     * Gửi tin nhắn
     */
    @Transactional
    public ChatMessageResponse sendMessage(ChatMessageRequest request, UUID senderId) {
        log.info("Sending message from {} to {}", senderId, request.getRecipientId());

        Users sender = userRepository.findById(senderId)
                .orElseThrow(() -> new AppException(ErrorStatus.USER_NOT_FOUND, "Sender not found"));

        UUID recipientId = UUID.fromString(request.getRecipientId());
        Users recipient = userRepository.findById(recipientId)
                .orElseThrow(() -> new AppException(ErrorStatus.USER_NOT_FOUND, "Recipient not found"));

        ChatMessage message = ChatMessage.builder()
                .sender(sender)
                .recipient(recipient)
                .content(request.getContent())
                .isRead(false)
                .build();

        ChatMessage saved = chatMessageRepository.save(message);
        log.info("Message saved with ID: {}", saved.getId());

        return toChatMessageResponse(saved);
    }

    /**
     * Lấy lịch sử chat với một người
     */
    @Transactional(readOnly = true)
    public Page<ChatMessageResponse> getChatHistory(UUID currentUserId, UUID otherUserId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("sentAt").descending());
        Page<ChatMessage> messages = chatMessageRepository.findConversation(currentUserId, otherUserId, pageable);

        return messages.map(this::toChatMessageResponse);
    }

    /**
     * Lấy danh sách người đã từng chat
     */
    @Transactional(readOnly = true)
    public List<ChatConversationResponse> getChatPartners(UUID userId) {
        log.info("Fetching chat partners for user: {}", userId);

        List<Users> partners = chatMessageRepository.findChatPartners(userId);

        return partners.stream()
                .map(partner -> {
                    ChatMessage lastMessage = chatMessageRepository.findLastMessage(userId, partner.getId());
                    long unreadCount = chatMessageRepository.countUnreadFrom(userId, partner.getId());
                    UserStatus status = userStatusRepository.findByUserId(partner.getId()).orElse(null);

                    return ChatConversationResponse.builder()
                            .userId(partner.getId())
                            .name(partner.getName())
                            .avatar(partner.getAvatar_url())
                            .lastMessage(lastMessage != null ? lastMessage.getContent() : "")
                            .lastMessageTime(lastMessage != null ? lastMessage.getSentAt() : null)
                            .unreadCount(unreadCount)
                            .status(status != null ? status.getStatus() : null)
                            .lastSeenAt(status != null ? status.getLastSeenAt() : null)
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * Đánh dấu tin nhắn đã đọc
     */
    @Transactional
    public void markAsRead(UUID messageId) {
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new AppException(ErrorStatus.MESSAGE_NOT_FOUND, "Message not found"));

        message.setRead(true);
        message.setReadAt(LocalDateTime.now());
        chatMessageRepository.save(message);

        log.info("Message {} marked as read", messageId);
    }

    /**
     * Đánh dấu tất cả tin nhắn từ một người là đã đọc
     */
    @Transactional
    public void markAllAsReadFrom(UUID currentUserId, UUID senderId) {
        Page<ChatMessage> unreadMessages = chatMessageRepository.findConversation(
                currentUserId, senderId, PageRequest.of(0, 1000));

        unreadMessages.getContent().stream()
                .filter(msg -> !msg.isRead() && msg.getRecipient().getId().equals(currentUserId))
                .forEach(msg -> {
                    msg.setRead(true);
                    msg.setReadAt(LocalDateTime.now());
                    chatMessageRepository.save(msg);
                });
    }

    /**
     * Lấy số tin nhắn chưa đọc
     */
    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        return chatMessageRepository.countUnreadMessages(userId);
    }

    /**
     * Convert ChatMessage to Response
     */
    private ChatMessageResponse toChatMessageResponse(ChatMessage message) {
        return ChatMessageResponse.builder()
                .id(message.getId())
                .senderId(message.getSender().getId())
                .senderName(message.getSender().getName())
                .senderAvatar(message.getSender().getAvatar_url())
                .recipientId(message.getRecipient().getId())
                .recipientName(message.getRecipient().getName())
                .content(message.getContent())
                .sentAt(message.getSentAt())
                .isRead(message.isRead())
                .readAt(message.getReadAt())
                .build();
    }
}
