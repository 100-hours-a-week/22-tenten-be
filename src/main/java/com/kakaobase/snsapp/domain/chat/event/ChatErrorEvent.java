package com.kakaobase.snsapp.domain.chat.event;

import com.kakaobase.snsapp.domain.chat.exception.errorcode.ChatErrorCode;
import lombok.Getter;

/**
 * 채팅 에러 이벤트
 * AI 서버 통신 에러 등 비동기 에러 발생 시 사용자에게 알림을 위한 이벤트
 */
@Getter
public class ChatErrorEvent extends ChatEvent {
    
    /**
     * 에러 코드
     */
    private final ChatErrorCode errorCode;
    
    /**
     * 에러 메시지
     */
    private final String errorMessage;
    
    /**
     * 요청 식별자 (StreamId, UserId 등)
     */
    private final String requestId;
    
    /**
     * 생성자
     */
    public ChatErrorEvent(Long userId, ChatErrorCode errorCode, String errorMessage, String requestId) {
        super(userId);
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.requestId = requestId;
    }
}