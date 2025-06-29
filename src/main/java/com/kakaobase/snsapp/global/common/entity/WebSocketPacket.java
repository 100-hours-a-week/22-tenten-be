package com.kakaobase.snsapp.global.common.entity;

import lombok.Builder;

@Builder
public abstract class WebSocketPacket<T> {
    String event;  // 세부 동작 (ex: liked, read, joined, error)
    T data;       // 실제 메시지 데이터 (개별 DTO로 분기)

    protected WebSocketPacket(String event, T data) {
        this.event = event;
        this.data = data;
    }

}
