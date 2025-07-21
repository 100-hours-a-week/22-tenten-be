package com.kakaobase.snsapp.domain.notification.error;

import lombok.Getter;

@Getter
public enum NotificationErrorCode {
    NOTIFICATION_FETCH_FAIL( "알림 데이터 조회에 실패하였습니다"),
    NOTIFICATION_DELETE_FAIL("알림 데이터 삭제에 실패하였습니다"),
    NOTIFICATION_NOT_FOUND("notification.error", "resource_not_found","해당 알림을 찾는데 실패하였습니다"),
    NOTIFICATION_UPDATE_FAIL("알림 데이터 업데이트에 실패하였습니다"),
    NOTIFICATION_READ_FAIL("notification.read.nack","internal_server_error","알림 데이터 읽기에 실패하였습니다"),
    NOTIFICATION_SEND_FAIL("알림 전송에 실패하였습니다.");


    private final String event;
    private final String error;
    private final String message;

    NotificationErrorCode(String message) {
        this.event = "notification.error";
        this.error = "internal_server_error";
        this.message = message;
    }

    NotificationErrorCode(String event, String error, String message) {
        this.event = event;
        this.error = error;
        this.message = message;
    }
}
