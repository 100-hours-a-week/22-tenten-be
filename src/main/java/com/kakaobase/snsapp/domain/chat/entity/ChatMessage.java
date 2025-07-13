package com.kakaobase.snsapp.domain.chat.entity;

import com.kakaobase.snsapp.domain.members.entity.Member;
import com.kakaobase.snsapp.global.common.entity.BaseSoftDeletableEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

/**
 * 채팅 메시지 엔티티
 * 채팅방 내에서 주고받는 메시지 정보를 저장합니다.
 */
@Entity
@Table(
        name = "chat_messages",
        indexes = {
                @Index(name = "idx_chat_room_id", columnList = "chat_room_id"),
                @Index(name = "idx_member_id", columnList = "member_id"),
                @Index(name = "idx_created_at", columnList = "created_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@DynamicInsert
@SQLDelete(sql = "UPDATE chat_messages SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@Where(clause = "deleted_at IS NULL")
public class ChatMessage extends BaseSoftDeletableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 채팅 내용
     */
    @Column(nullable = false, length = 255)
    private String content;

    /**
     * 확인 여부
     */
    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    /**
     * 메시지 발신자 (N:1)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    /**
     * 메시지가 속한 채팅방 (N:1)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    @Builder
    public ChatMessage(String content, Boolean isRead, Member member, ChatRoom chatRoom) {
        this.content = content;
        this.isRead = isRead != null ? isRead : false;
        this.member = member;
        this.chatRoom = chatRoom;
    }

    // === 비즈니스 로직 ===

    /**
     * 메시지 읽음 상태를 변경합니다.
     */
    public void markAsRead() {
        this.isRead = true;
    }

    /**
     * 메시지 내용을 수정합니다.
     *
     * @param content 새로운 메시지 내용
     */
    public void updateContent(String content) {
        this.content = content;
    }
}