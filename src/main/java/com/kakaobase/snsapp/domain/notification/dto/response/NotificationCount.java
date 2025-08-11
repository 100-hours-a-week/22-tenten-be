package com.kakaobase.snsapp.domain.notification.dto.response;

import lombok.Builder;

@Builder
public record NotificationCount(
        Long count
) {}
