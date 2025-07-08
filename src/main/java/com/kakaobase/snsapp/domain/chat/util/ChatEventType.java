package com.kakaobase.snsapp.domain.chat.util;

import lombok.Getter;

/**
 * 채팅 WebSocket 이벤트 타입 열거형
 */
@Getter
public enum ChatEventType {
    
    // 클라이언트 → 서버 이벤트
    CHAT_SEND("chat.send"),
    CHAT_TYPING("chat.typing"), 
    CHAT_STOP("chat.stop"),
    CHAT_STREAM_END_ACK("chat.stream.end.ack"),
    CHAT_STREAM_END_NACK("chat.stream.end.nack"),
    
    // 서버 → 클라이언트 이벤트
    CHAT_MESSAGE_LOADING("chat.loading"),
    CHAT_MESSAGE_STREAM_START("chat.stream.start"),
    CHAT_MESSAGE_STREAM("chat.stream"),
    CHAT_MESSAGE_STREAM_END("chat.stream.end"),
    CHAT_MESSAGE_STREAM_ERROR("chat.stream.error");
    
    private final String event;
    
    ChatEventType(String event) {
        this.event = event;
    }
}