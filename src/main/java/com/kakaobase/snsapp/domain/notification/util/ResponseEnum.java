package com.kakaobase.snsapp.domain.notification.util;

public enum ResponseEnum {
    READ_SUCCESS("notification.read.ack","알림 읽음 처리에 성공하였습니다."),
    READ_FAIL("notification.read.ack", "알림 읽음 처리에 실패하였습니다"),
    REMOVE_SUCCESS("notificaiton.remove.ack", "알림 삭제 처리에 성공하였습니다"),
    REMOVE_FAIL("noficiaiton.remove.nack", "알림 삭제 처리에 실패하였습니다");

    String event;
    String message;

    ResponseEnum(String event, String message) {
        this.event = event;
        this.message = message;
    }
}
