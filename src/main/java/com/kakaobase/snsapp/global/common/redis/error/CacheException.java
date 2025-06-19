package com.kakaobase.snsapp.global.common.redis.error;

import com.kakaobase.snsapp.global.error.code.BaseErrorCode;
import com.kakaobase.snsapp.global.error.exception.CustomException;

public class CacheException extends CustomException {

    public CacheException(BaseErrorCode errorCode) {
        super(errorCode);
    }

    public CacheException(BaseErrorCode errorCode, String message) {
        super(errorCode, null, message);
    }
}
