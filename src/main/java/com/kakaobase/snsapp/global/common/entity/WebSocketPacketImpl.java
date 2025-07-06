package com.kakaobase.snsapp.global.common.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class WebSocketPacketImpl<T> extends WebSocketPacket<T> {
    
    @JsonCreator
    public WebSocketPacketImpl(@JsonProperty("event") String event, @JsonProperty("data") T data) {
        super(event, data);
    }
}