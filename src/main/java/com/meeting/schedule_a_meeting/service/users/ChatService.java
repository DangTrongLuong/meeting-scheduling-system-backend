package com.meeting.schedule_a_meeting.service.users;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meeting.schedule_a_meeting.dto.request.users.chat.ChatMessageRequest;
import com.meeting.schedule_a_meeting.dto.response.users.chat.ChatConversationResponse;
import com.meeting.schedule_a_meeting.dto.response.users.chat.ChatMessageResponse;
import com.meeting.schedule_a_meeting.dto.response.users.meeting.UserSummary;
import com.meeting.schedule_a_meeting.entities.ChatMessage;
import com.meeting.schedule_a_meeting.entities.UserStatus;
import com.meeting.schedule_a_meeting.entities.Users;
import com.meeting.schedule_a_meeting.enums.ErrorStatus;
import com.meeting.schedule_a_meeting.exception.AppException;
import com.meeting.schedule_a_meeting.repositories.UserRepository;
import com.meeting.schedule_a_meeting.repositories.chat_call.ChatMessageRepository;
import com.meeting.schedule_a_meeting.repositories.chat_call.UserStatusRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final SimpMessagingTemplate messagingTemplate;
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

        ChatMessageResponse response = toChatMessageResponse(saved);

        // Gửi cho người nhận
        messagingTemplate.convertAndSendToUser(
                recipientId.toString(),
                "/topic/chat",
                response);

        // Gửi cho người gửi (để update UI của họ)
        messagingTemplate.convertAndSendToUser(
                senderId.toString(),
                "/topic/chat",
                response);

        log.info("Message broadcasted to both sender and recipient");

        return response;
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
     * Lấy danh sách người đã từng chat - FIXED VERSION
     */
    @Transactional(readOnly = true)
    public List<ChatConversationResponse> getChatPartners(UUID userId) {
        log.info("Fetching chat partners for user: {}", userId);

        try {
            // Lấy danh sách byte[] của các partner IDs
            List<byte[]> partnerIdBytes = chatMessageRepository.findChatPartnerIds(userId.toString());

            if (partnerIdBytes.isEmpty()) {
                return List.of();
            }

            // Chuyển byte[] thành UUID và lấy users
            return partnerIdBytes.stream()
                    .map(bytes -> {
                        try {
                            return new UUID(
                                    ((long) bytes[0] << 56) | ((long) (bytes[1] & 255) << 48)
                                            | ((long) (bytes[2] & 255) << 40) | ((long) (bytes[3] & 255) << 32) |
                                            ((long) (bytes[4] & 255) << 24) | ((long) (bytes[5] & 255) << 16)
                                            | ((long) (bytes[6] & 255) << 8) | (bytes[7] & 255),
                                    ((long) bytes[8] << 56) | ((long) (bytes[9] & 255) << 48)
                                            | ((long) (bytes[10] & 255) << 40) | ((long) (bytes[11] & 255) << 32) |
                                            ((long) (bytes[12] & 255) << 24) | ((long) (bytes[13] & 255) << 16)
                                            | ((long) (bytes[14] & 255) << 8) | (bytes[15] & 255));
                        } catch (Exception e) {
                            log.warn("Error converting bytes to UUID", e);
                            return null;
                        }
                    })
                    .filter(id -> id != null && !id.equals(userId))
                    .distinct()
                    .map(partnerId -> {
                        Users partner = userRepository.findById(partnerId).orElse(null);
                        if (partner == null)
                            return null;

                        // Lấy tin nhắn cuối cùng
                        ChatMessage lastMessage = chatMessageRepository.findLastMessage(userId, partnerId);

                        // Đếm tin chưa đọc
                        long unreadCount = chatMessageRepository.countUnreadFrom(userId, partnerId);

                        // Lấy trạng thái user
                        UserStatus status = userStatusRepository.findByUserId(partnerId).orElse(null);

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
                    .filter(conv -> conv != null)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error fetching chat partners: ", e);
            return List.of();
        }
    }

    /**
     * Đánh dấu tin nhắn đã đọc
     */
    @Transactional
    public void markAsRead(UUID messageId) {
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new AppException(ErrorStatus.MESSAGE_NOT_FOUND, "Message not found"));

        if (message.isRead()) {
            return; // Đã đọc rồi thì không làm gì
        }

        message.setRead(true);
        message.setReadAt(LocalDateTime.now());
        chatMessageRepository.save(message);

        log.info("Message {} marked as read", messageId);

        // Broadcast cập nhật trạng thái đã đọc cho cả hai bên
        ChatMessageResponse response = toChatMessageResponse(message);

        messagingTemplate.convertAndSendToUser(
                message.getSender().getId().toString(),
                "/topic/chat",
                response);

        messagingTemplate.convertAndSendToUser(
                message.getRecipient().getId().toString(),
                "/topic/chat",
                response);
    }

    /**
     * Đánh dấu tất cả tin nhắn từ một người là đã đọc
     */
    @Transactional
    public void markAllAsReadFrom(UUID currentUserId, UUID senderId) {
        log.info("Marking all messages from {} as read for user {}", senderId, currentUserId);

        // Lấy tất cả tin nhắn chưa đọc từ senderId gửi đến currentUserId
        Page<ChatMessage> unreadPage = chatMessageRepository.findConversation(
                currentUserId, senderId, PageRequest.of(0, 1000, Sort.by("sentAt").ascending()));

        List<ChatMessage> unreadMessages = unreadPage.getContent().stream()
                .filter(msg -> msg.getRecipient().getId().equals(currentUserId) && !msg.isRead())
                .toList();

        if (unreadMessages.isEmpty()) {
            log.info("No unread messages found from {} to {}", senderId, currentUserId);
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        // Cập nhật trạng thái đã đọc
        unreadMessages.forEach(msg -> {
            msg.setRead(true);
            msg.setReadAt(now);
        });

        // Lưu toàn bộ một lần (tối ưu hơn save từng cái)
        chatMessageRepository.saveAll(unreadMessages);

        log.info("Marked {} messages as read from {}", unreadMessages.size(), senderId);

        // Broadcast realtime cập nhật cho cả hai bên
        List<ChatMessageResponse> updatedResponses = unreadMessages.stream()
                .map(this::toChatMessageResponse)
                .toList();

        // Gửi cho người nhận (currentUserId) - cập nhật giao diện của họ
        messagingTemplate.convertAndSendToUser(
                currentUserId.toString(),
                "/topic/chat",
                updatedResponses);

        // Gửi cho người gửi (senderId) - để họ thấy dấu ✓✓ ngay lập tức
        messagingTemplate.convertAndSendToUser(
                senderId.toString(),
                "/topic/chat",
                updatedResponses);
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
                .sentAt(message.getSentAt() != null ? message.getSentAt() : LocalDateTime.now()) // ✅ FIX
                .isRead(message.isRead())
                .readAt(message.getReadAt())
                .build();
    }

    @Transactional(readOnly = true)
    public List<UserSummary> searchUsers(String keyword, UUID currentUserId) {
        log.info("Searching users with keyword: {}", keyword);

        try {
            // Tìm users theo email hoặc name (không lấy chính user)
            List<Users> users = userRepository.findByNameContainingIgnoreCaseOrEmailContainingIgnoreCaseAndIdNot(
                    keyword, keyword, currentUserId);

            return users.stream()
                    .limit(20) // Max 20 results
                    .map(user -> UserSummary.builder()
                            .id(user.getId())
                            .name(user.getName())
                            .email(user.getEmail())
                            .avatarUrl(user.getAvatar_url())
                            .build())
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error searching users: ", e);
            return List.of();
        }
    }
}