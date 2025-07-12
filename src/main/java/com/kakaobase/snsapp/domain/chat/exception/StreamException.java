package com.kakaobase.snsapp.domain.chat.exception;

import com.kakaobase.snsapp.domain.chat.exception.errorcode.StreamErrorCode;
import lombok.Getter;

@Getter
public class StreamException extends RuntimeException {

    private final transient StreamErrorCode errorCode;
    private final transient Long userId;

    public StreamException(StreamErrorCode errorCode, Long userId) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.userId = userId;
    }

    public StreamException(StreamErrorCode errorCode, Long userId, String aiServerId, String streamId) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.userId = userId;
    }
}