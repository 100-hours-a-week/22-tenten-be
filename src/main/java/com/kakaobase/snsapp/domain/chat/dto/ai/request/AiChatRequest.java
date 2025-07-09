package com.kakaobase.snsapp.domain.chat.dto.ai.request;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record AiChatRequest(
        Long userId,
        String message,
        LocalDateTime timestamp
) {
}