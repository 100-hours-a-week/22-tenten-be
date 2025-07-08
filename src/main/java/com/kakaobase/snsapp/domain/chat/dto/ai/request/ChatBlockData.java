package com.kakaobase.snsapp.domain.chat.dto.ai.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
//Ai서버에게 보낼 채팅 묶음 Dto
public record ChatBlockData(
        @JsonProperty("chat_id")
        Long chatId,
        @JsonProperty("sender_id")
        Long senderId,
        @JsonProperty("nickname")
        String nickname,
        @JsonProperty("class_name")
        String className,
        @JsonProperty("content")
        String content,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime timestamp,
        @JsonProperty("is_read")
        Boolean isRead
) {}
