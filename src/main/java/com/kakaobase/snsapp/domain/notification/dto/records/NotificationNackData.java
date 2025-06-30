package com.kakaobase.snsapp.domain.notification.dto.records;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record NotificationNackData(
        Long id,
        String error,
        String message,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime timestamp
) {}
