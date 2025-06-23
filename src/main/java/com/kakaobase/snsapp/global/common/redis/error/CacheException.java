package com.kakaobase.snsapp.global.common.redis.error;
import lombok.Getter;

@Getter
public class CacheException extends RuntimeException {

    private final transient CacheErrorCode errorCode;

    public CacheException(CacheErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public CacheException(CacheErrorCode errorCode, String message) {
        super(errorCode.getMessage()+message);
        this.errorCode = errorCode;
    }
}
