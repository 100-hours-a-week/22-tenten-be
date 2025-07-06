package com.kakaobase.snsapp.domain.notification.util;

import com.kakaobase.snsapp.global.common.entity.WebSocketPacket;

import java.util.List;

/**
 * 알림 필터링 결과를 담는 레코드
 */
public record FilterResult(
        List<WebSocketPacket<?>> validNotifications,
        List<Long> invalidNotificationIds
) {}