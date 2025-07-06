package com.kakaobase.snsapp.domain.notification.dto.records;

import com.kakaobase.snsapp.global.common.entity.WebSocketPacket;

import java.util.List;

public record NotificationFetchData(
        int unread_count,
        List<WebSocketPacket<?>>notifications
) {}
