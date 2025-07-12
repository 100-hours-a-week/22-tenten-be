package com.kakaobase.snsapp.domain.chat.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/**
 * 채팅 Stream ACK/NACK 데이터 통합 DTO
 */
public record ChatAckData(
        @JsonProperty("chat_id")
        Long chatId,
        @JsonProperty("message")
        String message,
        @JsonProperty("timestamp") @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime timestamp
) {}