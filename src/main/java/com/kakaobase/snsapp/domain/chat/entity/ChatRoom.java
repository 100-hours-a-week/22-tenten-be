package com.kakaobase.snsapp.domain.chat.entity;

import com.kakaobase.snsapp.global.common.entity.BaseCreatedTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicInsert;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * 채팅방 엔티티
 * 각 채팅방에 대한 정보를 저장합니다.
 */
@Entity
@Table(name = "chat_rooms")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@DynamicInsert
public class ChatRoom extends BaseCreatedTimeEntity {

    @Id
    private Long id;

    /**
     * 마지막 메시지 ID
     * 채팅방의 마지막 메시지 ID를 저장하여 조회 성능을 최적화합니다.
     */
    @Column(name = "last_message_id")
    private Long lastMessageId = null;

    /**
     * 마지막 메시지 전송 시각
     * 채팅방의 마지막 메시지가 전송된 시간을 저장합니다.
     */
    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt = null;

    @Builder
    public ChatRoom(Long id) {
        this.id = id;
    }

    // === 연관관계 매핑 ===

    /**
     * 채팅방 구성원들 - 중간 테이블을 통한 매핑 (1:N)
     */
    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private final Set<ChatRoomMember> chatRoomMembers = new HashSet<>();

    /**
     * 채팅 메시지들 (1:N)
     */
    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private final Set<ChatMessage> messages = new HashSet<>();

    /**
     * 마지막 메시지 정보를 업데이트합니다
     */
    public void updateLastMessage(ChatMessage lastMessage) {
        this.lastMessageId = lastMessage.getId();
        this.lastMessageAt = lastMessage.getCreatedAt();
    }
}