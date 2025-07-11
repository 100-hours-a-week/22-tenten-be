package com.kakaobase.snsapp.domain.chat.converter;

import com.kakaobase.snsapp.domain.chat.dto.request.ChatData;
import com.kakaobase.snsapp.domain.chat.dto.response.ChatErrorData;
import com.kakaobase.snsapp.domain.chat.dto.response.ChatItemDto;
import com.kakaobase.snsapp.domain.chat.entity.ChatMessage;
import com.kakaobase.snsapp.domain.chat.entity.ChatRoom;
import com.kakaobase.snsapp.domain.chat.exception.ChatException;
import com.kakaobase.snsapp.domain.chat.exception.StreamException;
import com.kakaobase.snsapp.domain.chat.exception.errorcode.StreamErrorCode;
import com.kakaobase.snsapp.domain.members.entity.Member;
import com.kakaobase.snsapp.global.common.constant.BotConstants;
import com.kakaobase.snsapp.global.common.entity.WebSocketPacketImpl;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class ChatConverter {

    private final EntityManager em;

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

    public ChatMessage toChatMessage(Long senderId, String content) {
        ChatRoom chatRoom = em.getReference(ChatRoom.class, senderId);
        Member sender = em.getReference(Member.class, senderId);

        return ChatMessage.builder()
                .content(content)
                .isRead(true) //유저가 보낸 채팅은 무조건 읽음 처리
                .member(sender)
                .chatRoom(chatRoom)
                .build();
    }

    /**
     * AI 봇 메시지 생성을 위한 헬퍼 메서드
     */
    public ChatMessage toBotMessage(Long userId, String content) {
        ChatRoom chatRoom = em.getReference(ChatRoom.class, userId);
        Member botMember = em.getReference(Member.class, BotConstants.BOT_MEMBER_ID);

        return ChatMessage.builder()
                .content(content)
                .isRead(false)
                .member(botMember)
                .chatRoom(chatRoom)
                .build();
    }

    public WebSocketPacketImpl<ChatErrorData> toErrorPacket(ChatException e) {
        ChatErrorData errorData = ChatErrorData.builder()
                .error(e.getErrorCode().getError())
                .message(e.getErrorCode().getMessage())
                .timestamp(LocalDateTime.now())
                .build();

        return new WebSocketPacketImpl<>(e.getErrorCode().getEvent(), errorData);
    }

    public WebSocketPacketImpl<ChatErrorData> toErrorPacket(StreamException e) {
        ChatErrorData errorData = ChatErrorData.builder()
                .error(e.getErrorCode().getError())
                .message(e.getErrorCode().getMessage())
                .timestamp(LocalDateTime.now())
                .build();

        return new WebSocketPacketImpl<>(e.getErrorCode().getEvent(), errorData);
    }

    public WebSocketPacketImpl<ChatErrorData> toErrorPacket(StreamErrorCode errorEnum) {
        ChatErrorData errorData = ChatErrorData.builder()
                .error(errorEnum.getError())
                .message(errorEnum.getMessage())
                .timestamp(LocalDateTime.now())
                .build();

        return new WebSocketPacketImpl<>(errorEnum.getEvent(), errorData);
    }

}