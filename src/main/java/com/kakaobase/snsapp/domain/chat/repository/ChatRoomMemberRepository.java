package com.kakaobase.snsapp.domain.chat.repository;

import com.kakaobase.snsapp.domain.chat.entity.ChatRoomMember;
import com.kakaobase.snsapp.domain.chat.entity.ChatRoomMember.ChatRoomMemberId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, ChatRoomMemberId> {

    /**
     * 채팅방 멤버 존재 여부 확인
     */
    @Query("SELECT COUNT(crm) > 0 FROM ChatRoomMember crm " +
           "WHERE crm.id.chatRoomId = :chatRoomId AND crm.id.memberId = :memberId")
    boolean existsByChatRoomIdAndMemberId(@Param("chatRoomId") Long chatRoomId, @Param("memberId") Long memberId);
}