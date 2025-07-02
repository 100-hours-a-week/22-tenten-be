package com.kakaobase.snsapp.global.error.code;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ErrorPacketData(
        String error,
        String message,
        LocalDateTime timestamp
)
{}
