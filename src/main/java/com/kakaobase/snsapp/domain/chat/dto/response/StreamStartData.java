package com.kakaobase.snsapp.domain.chat.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.time.LocalDateTime;


/**
 * 스트리밍 시작시 전송할 data
 * events는 chat.stream.start 고정
 */
@Builder
public record StreamStartData(
        @JsonProperty("stream_id")
        String streamId,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime timestamp
) {}
