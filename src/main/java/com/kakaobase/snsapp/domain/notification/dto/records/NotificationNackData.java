package com.kakaobase.snsapp.domain.notification.dto.records;

import java.time.LocalDateTime;

public record NotificationNackData(
        Long id,
        String error,
        String message,
        LocalDateTime timestamp
) {}
