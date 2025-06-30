package com.kakaobase.snsapp.domain.notification.dto.packets;

import com.kakaobase.snsapp.domain.notification.dto.records.NotificationNackData;
import com.kakaobase.snsapp.global.common.entity.WebSocketPacket;
import lombok.experimental.SuperBuilder;

@SuperBuilder
public class NotificationNackPacket extends WebSocketPacket<NotificationNackData> {
    NotificationNackPacket(String event, NotificationNackData data) {
        super(event, data);
    }
}
