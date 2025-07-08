package com.kakaobase.snsapp.domain.chat.dto.websocket;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/**
 * 채팅 ACK/NACK 데이터 통합 DTO
 * event: "chat.stream.end.ack", "chat.stream.end.nack" 모두 사용
 */
public record ChatAckData(
        @JsonProperty("chat_id")
        Long chatId,
        @JsonProperty("message")
        String message,
        @JsonProperty("timestamp") @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime timestamp
) {}