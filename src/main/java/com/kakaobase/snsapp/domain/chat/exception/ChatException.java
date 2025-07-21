package com.kakaobase.snsapp.domain.chat.exception;

import com.kakaobase.snsapp.domain.chat.exception.errorcode.ChatErrorCode;
import lombok.Getter;

@Getter
public class ChatException extends RuntimeException {

    private final transient ChatErrorCode errorCode;
    private final transient Long userId;

    public ChatException(ChatErrorCode errorCode,Long userId) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.userId = userId;
    }
}