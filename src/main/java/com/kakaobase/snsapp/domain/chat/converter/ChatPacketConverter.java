package com.kakaobase.snsapp.domain.chat.converter;

import com.kakaobase.snsapp.domain.chat.dto.ai.response.AiStreamData;
import com.kakaobase.snsapp.domain.chat.dto.websocket.ChatAckData;
import com.kakaobase.snsapp.domain.chat.util.ChatEventType;
import com.kakaobase.snsapp.global.common.entity.WebSocketPacketImpl;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class ChatPacketConverter {

    /**
     * Loading 상태 WebSocket 패킷 생성
     */
    public WebSocketPacketImpl<ChatAckData> createLoadingPacket(String message) {
        ChatAckData data = new ChatAckData(null, message, LocalDateTime.now());
        return new WebSocketPacketImpl<>(ChatEventType.CHAT_MESSAGE_LOADING.getEvent(), data);
    }

    /**
     * 스트림 시작 WebSocket 패킷 생성
     */
    public WebSocketPacketImpl<ChatAckData> createStreamStartPacket(Long chatId) {
        ChatAckData data = new ChatAckData(chatId, "스트리밍 시작", LocalDateTime.now());
        return new WebSocketPacketImpl<>(ChatEventType.CHAT_MESSAGE_STREAM_START.getEvent(), data);
    }

    /**
     * 스트림 데이터 WebSocket 패킷 생성
     */
    public WebSocketPacketImpl<ChatAckData> createStreamDataPacket(Long chatId, String content) {
        ChatAckData data = new ChatAckData(chatId, content, LocalDateTime.now());
        return new WebSocketPacketImpl<>(ChatEventType.CHAT_MESSAGE_STREAM.getEvent(), data);
    }

    /**
     * 스트림 완료 WebSocket 패킷 생성
     */
    public WebSocketPacketImpl<ChatAckData> createStreamEndPacket(Long chatId) {
        ChatAckData data = new ChatAckData(chatId, "스트리밍 완료", LocalDateTime.now());
        return new WebSocketPacketImpl<>(ChatEventType.CHAT_MESSAGE_STREAM_END.getEvent(), data);
    }

    /**
     * 에러 WebSocket 패킷 생성
     */
    public WebSocketPacketImpl<ChatAckData> createErrorPacket(String errorMessage) {
        ChatAckData data = new ChatAckData(null, errorMessage, LocalDateTime.now());
        return new WebSocketPacketImpl<>(ChatEventType.CHAT_MESSAGE_STREAM_ERROR.getEvent(), data);
    }

    /**
     * AI 서버 스트림 데이터를 WebSocket 패킷으로 변환
     */
    public WebSocketPacketImpl<ChatAckData> fromAiStreamData(AiStreamData aiData) {
        ChatAckData data = new ChatAckData(
                null, // chatId는 AI 데이터에 없으므로 null
                aiData.message(), 
                aiData.timestamp()
        );
        return new WebSocketPacketImpl<>(ChatEventType.CHAT_MESSAGE_STREAM.getEvent(), data);
    }

    /**
     * 채팅 중단 WebSocket 패킷 생성
     */
    public WebSocketPacketImpl<ChatAckData> createStopPacket(String reason) {
        ChatAckData data = new ChatAckData(null, reason, LocalDateTime.now());
        return new WebSocketPacketImpl<>(ChatEventType.CHAT_MESSAGE_LOADING.getEvent(), data);
    }
}