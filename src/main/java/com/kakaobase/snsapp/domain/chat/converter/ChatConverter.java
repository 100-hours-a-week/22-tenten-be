package com.kakaobase.snsapp.domain.chat.converter;

import com.kakaobase.snsapp.domain.chat.dto.request.ChatData;
import com.kakaobase.snsapp.domain.chat.dto.response.ChatItemDto;
import com.kakaobase.snsapp.domain.chat.entity.ChatMessage;
import com.kakaobase.snsapp.domain.chat.entity.ChatRoom;
import com.kakaobase.snsapp.domain.members.entity.Member;
import org.springframework.stereotype.Component;

@Component
public class ChatConverter {

    /**
     * ChatMessage Entity를 Response ChatData DTO로 변환
     */
    public ChatItemDto toChatItemDto(ChatMessage message) {
        return ChatItemDto.builder()
                .chatId(message.getId())
                .senderId(message.getMember().getId())
                .content(message.getContent())
                .timestamp(message.getCreatedAt())
                .isRead(message.getIsRead())
                .build();
    }

    /**
     * Request ChatData DTO를 ChatMessage Entity로 변환
     */
    public ChatMessage toChatMessage(ChatData requestData, Member sender, ChatRoom chatRoom) {
        return ChatMessage.builder()
                .content(requestData.content())
                .isRead(false) // 새 메시지는 기본적으로 읽지 않음
                .member(sender)
                .chatRoom(chatRoom)
                .build();
    }

    public ChatMessage toChatMessage(String content, Member sender, ChatRoom chatRoom) {
        return ChatMessage.builder()
                .content(content)
                .isRead(false) // 새 메시지는 기본적으로 읽지 않음
                .member(sender)
                .chatRoom(chatRoom)
                .build();
    }

    /**
     * AI 봇 메시지 생성을 위한 헬퍼 메서드
     */
    public ChatMessage createBotMessage(String content, Member botMember, ChatRoom chatRoom) {
        return ChatMessage.builder()
                .content(content)
                .isRead(false)
                .member(botMember)
                .chatRoom(chatRoom)
                .build();
    }
}