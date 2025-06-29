package com.kakaobase.snsapp.domain.notification.dto.packets;

import com.kakaobase.snsapp.global.common.entity.WebSocketPacket;

public class NotificationAckPacket extends WebSocketPacket<NotificationAckPacket> {
    NotificationAckPacket(String event, NotificationAckPacket data) {
        super(event, data);
    }
}
