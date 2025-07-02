package com.kakaobase.snsapp.domain.notification.error;

import com.kakaobase.snsapp.global.error.code.BaseErrorCode;
import com.kakaobase.snsapp.global.error.exception.CustomException;

public class NotificationException extends CustomException {
    public NotificationException(BaseErrorCode errorCode) {
        super(errorCode);
    }
    public NotificationException(BaseErrorCode errorCode, String field) {
        super(errorCode, field);
    }
    public NotificationException(BaseErrorCode errorCode, String field, String additionalMessage) {
        super(errorCode, field, additionalMessage);
    }
}
