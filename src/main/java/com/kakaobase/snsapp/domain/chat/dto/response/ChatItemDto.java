package com.kakaobase.snsapp.domain.chat.dto.response;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

/**
 * 사용자가 http로 채팅 조회시 반환할 Item
 */
@Builder
public record ChatItemDto(
        @JsonProperty("chat_id")
        Long chatId,
        @JsonProperty("sender_id")
        Long senderId,
        @JsonProperty("content")
        String content,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime timestamp,
        @JsonProperty("is_read")
        Boolean isRead
) {}
