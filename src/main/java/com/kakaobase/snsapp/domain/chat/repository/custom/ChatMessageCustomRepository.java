package com.kakaobase.snsapp.domain.chat.repository.custom;

import com.kakaobase.snsapp.domain.chat.dto.response.ChatList;
import com.kakaobase.snsapp.domain.chat.entity.ChatMessage;

import java.util.List;

public interface ChatMessageCustomRepository {

    /**
     * 특정 채팅방의 메시지 목록 조회 (페이지네이션)
     * - DB에서 최신순으로 조회 후, List 내에서는 오래된 순으로 정렬
     * - hasNext 정보 포함하여 반환
     */
    ChatList findMessagesByChatRoomId(Long chatRoomId, Integer limit, Long cursor);
}