package com.meeting.schedule_a_meeting.repositories.chat_call;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.meeting.schedule_a_meeting.entities.ChatMessage;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

        // Lấy tin nhắn giữa 2 người (both directions)
        @Query("SELECT cm FROM ChatMessage cm WHERE " +
                        "((cm.sender.id = :userId1 AND cm.recipient.id = :userId2) OR " +
                        "(cm.sender.id = :userId2 AND cm.recipient.id = :userId1)) " +
                        "ORDER BY cm.sentAt DESC")
        Page<ChatMessage> findConversation(@Param("userId1") UUID userId1,
                        @Param("userId2") UUID userId2,
                        Pageable pageable);

        // Lấy danh sách người dùng đã chat (FIXED - Simple version)
        @Query(value = "SELECT DISTINCT CASE " +
                        "WHEN cm.sender_id = UNHEX(REPLACE(:userId, '-', '')) THEN cm.recipient_id " +
                        "ELSE cm.sender_id END as user_id " +
                        "FROM chat_messages cm " +
                        "WHERE cm.sender_id = UNHEX(REPLACE(:userId, '-', '')) OR cm.recipient_id = UNHEX(REPLACE(:userId, '-', ''))", nativeQuery = true)
        List<byte[]> findChatPartnerIds(@Param("userId") String userId);

        // Lấy tin nhắn chưa đọc
        @Query("SELECT COUNT(cm) FROM ChatMessage cm " +
                        "WHERE cm.recipient.id = :userId AND cm.isRead = false")
        long countUnreadMessages(@Param("userId") UUID userId);

        // Lấy tin nhắn chưa đọc từ một người cụ thể
        @Query("SELECT COUNT(cm) FROM ChatMessage cm " +
                        "WHERE cm.recipient.id = :recipientId AND cm.sender.id = :senderId AND cm.isRead = false")
        long countUnreadFrom(@Param("recipientId") UUID recipientId, @Param("senderId") UUID senderId);

        // Lấy tin nhắn cuối cùng giữa 2 người
        @Query("SELECT cm FROM ChatMessage cm WHERE " +
                        "((cm.sender.id = :userId1 AND cm.recipient.id = :userId2) OR " +
                        "(cm.sender.id = :userId2 AND cm.recipient.id = :userId1)) " +
                        "ORDER BY cm.sentAt DESC " +
                        "LIMIT 1")
        ChatMessage findLastMessage(@Param("userId1") UUID userId1, @Param("userId2") UUID userId2);
}