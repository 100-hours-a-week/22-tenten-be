package com.kakaobase.snsapp.domain.follow.exception;

import com.kakaobase.snsapp.global.error.code.BaseErrorCode;
import com.kakaobase.snsapp.global.error.exception.CustomException;

public class FollowException extends CustomException {

    public FollowException(BaseErrorCode errorCode) {
        super(errorCode);
    }

    public FollowException(BaseErrorCode errorCode, String field) {
        super(errorCode, field);
    }

    public FollowException(BaseErrorCode errorCode, String field, String additionalMessage) {
        super(errorCode, field, additionalMessage);
    }
}
