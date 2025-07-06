package com.kakaobase.snsapp.domain.notification.error;

import lombok.Getter;

@Getter
public class NotificationException extends RuntimeException {

    private final transient NotificationErrorCode erorrCode;

    public NotificationException(NotificationErrorCode errorCode) {
        super(errorCode.getMessage());
        this.erorrCode = errorCode;
    }
}
