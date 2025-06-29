package com.kakaobase.snsapp.domain.notification.dto.records;

import java.time.LocalDateTime;

public record NotificationAckData(
        Long id,
        String message,
        LocalDateTime timestamp
) {}