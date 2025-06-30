package com.kakaobase.snsapp.domain.notification.dto.packets;

import com.kakaobase.snsapp.domain.notification.dto.records.NotificationRequestData;
import com.kakaobase.snsapp.global.common.entity.WebSocketPacket;
import lombok.experimental.SuperBuilder;

@SuperBuilder
public class NotificationRequestPacket extends WebSocketPacket<NotificationRequestData> {

    NotificationRequestPacket(String event, NotificationRequestData data) {
        super(event, data);
    }
}
