package com.kakaobase.snsapp.global.common.entity;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonSubTypes;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", defaultImpl = WebSocketPacketImpl.class)
@JsonSubTypes({
    @JsonSubTypes.Type(value = WebSocketPacketImpl.class, name = "default")
})
public abstract class WebSocketPacket<T> {
    public final String event;  // 세부 동작 (ex: liked, read, joined, error)
    public final T data;       // 실제 메시지 데이터 (개별 DTO로 분기)

    protected WebSocketPacket(String event, T data) {
        this.event = event;
        this.data = data;
    }
}
