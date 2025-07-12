package com.kakaobase.snsapp.domain.chat.entity;

import com.kakaobase.snsapp.domain.members.entity.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 채팅방 구성원 엔티티
 * Member와 ChatRoom 사이의 다대다 관계를 나타내는 중간 테이블입니다.
 */
@Entity
@Table(
        name = "chat_room_members",
        indexes = {
                @Index(name = "idx_member_id", columnList = "member_id"),
                @Index(name = "idx_chat_room_id", columnList = "chat_room_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoomMember {

    @EmbeddedId
    private ChatRoomMemberId id;

    /**
     * 회원 엔티티 (N:1)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", insertable = false, updatable = false)
    private Member member;

    /**
     * 채팅방 엔티티 (N:1)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", insertable = false, updatable = false)
    private ChatRoom chatRoom;

    /**
     * 채팅방 구성원 정보 생성을 위한 생성자
     *
     * @param member 구성원 회원
     * @param chatRoom 채팅방
     */
    public ChatRoomMember(Member member, ChatRoom chatRoom) {
        this.id = new ChatRoomMemberId(member.getId(), chatRoom.getId());
        this.member = member;
        this.chatRoom = chatRoom;
    }

    /**
     * ChatRoomMember 엔티티의 임베디드 복합 기본키 클래스
     */
    @Embeddable
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class ChatRoomMemberId implements java.io.Serializable {

        @Column(name = "member_id")
        private Long memberId;

        @Column(name = "chat_room_id")
        private Long chatRoomId;

        public ChatRoomMemberId(Long memberId, Long chatRoomId) {
            this.memberId = memberId;
            this.chatRoomId = chatRoomId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ChatRoomMemberId that = (ChatRoomMemberId) o;

            if (!memberId.equals(that.memberId)) return false;
            return chatRoomId.equals(that.chatRoomId);
        }

        @Override
        public int hashCode() {
            int result = memberId.hashCode();
            result = 31 * result + chatRoomId.hashCode();
            return result;
        }
    }
}