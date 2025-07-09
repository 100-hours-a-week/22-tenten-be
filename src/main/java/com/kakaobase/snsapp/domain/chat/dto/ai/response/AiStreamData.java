package com.kakaobase.snsapp.domain.chat.dto.ai.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * AI 서버 응답 데이터 통합 DTO
 * event: "stream", "done", "error" 모두 사용
 */
@Builder
public record AiStreamData(
        @JsonProperty("user_id")
        Long userId,
        @JsonProperty("message")
        String message,
        @JsonProperty("is_complete")
        boolean isComplete,
        @JsonProperty("timestamp")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime timestamp
) {}