package com.kakaobase.snsapp.domain.notification.error;

import lombok.Getter;

@Getter
public class NotificationException extends RuntimeException {

    private final transient NotificationErrorCode erorrCode;
    private final transient Long NotificationId;

    public NotificationException(NotificationErrorCode errorCode) {
        super(errorCode.getMessage());
        this.NotificationId = null;
        this.erorrCode = errorCode;
    }

    public NotificationException(NotificationErrorCode errorCode, Long notificationId) {
        super(errorCode.getMessage());
        this.NotificationId = notificationId;
        this.erorrCode = errorCode;
    }
}
