package com.kakaobase.snsapp.domain.chat.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 스트리밍 종료 요청시 오는 dataa
 * events는 chat.stop 고정
 */
@Builder
public record StreamStopData(
        @JsonProperty("stream_id")
        String streamId,
        LocalDateTime timestamp
) {}
