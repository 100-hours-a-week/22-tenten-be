package com.kakaobase.snsapp.domain.chat.repository;

import com.kakaobase.snsapp.domain.chat.entity.ChatMessage;
import com.kakaobase.snsapp.domain.chat.repository.custom.ChatMessageCustomRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long>, ChatMessageCustomRepository {

    /**
     * 특정 채팅방의 최신 메시지 조회
     */
    @Query("SELECT cm FROM ChatMessage cm " +
           "WHERE cm.chatRoom.id = :chatRoomId " +
           "ORDER BY cm.createdAt DESC LIMIT 1")
    ChatMessage findLatestMessageByChatRoomId(@Param("chatRoomId") Long chatRoomId);
}