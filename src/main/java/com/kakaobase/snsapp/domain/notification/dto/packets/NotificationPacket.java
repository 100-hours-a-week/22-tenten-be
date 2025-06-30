package com.kakaobase.snsapp.domain.notification.dto.packets;

import com.kakaobase.snsapp.domain.notification.dto.records.NotificationData;
import com.kakaobase.snsapp.global.common.entity.WebSocketPacket;
import lombok.experimental.SuperBuilder;

@SuperBuilder
public class NotificationPacket extends WebSocketPacket<NotificationData> {
    NotificationPacket(String event, NotificationData data) {
        super(event, data);
    }
}