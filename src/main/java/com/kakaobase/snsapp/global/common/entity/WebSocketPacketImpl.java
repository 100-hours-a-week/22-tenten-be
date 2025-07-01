package com.kakaobase.snsapp.global.common.entity;

public class WebSocketPacketImpl<T> extends WebSocketPacket<T> {
    
    public WebSocketPacketImpl(String event, T data) {
        super(event, data);
    }
}