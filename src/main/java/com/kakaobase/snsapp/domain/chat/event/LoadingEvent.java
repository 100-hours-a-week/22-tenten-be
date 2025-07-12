package com.kakaobase.snsapp.domain.chat.event;

import lombok.Getter;

/**
 * AI 서버로 ChatBlock 전송 시작 이벤트
 * 사용자에게 로딩 상태를 알리기 위한 이벤트
 */
@Getter
public class LoadingEvent extends ChatEvent {
    
    /**
     * 스트림 ID
     */
    private final String streamId;
    
    /**
     * 전송된 메시지 내용
     */
    private final String messageContent;
    
    /**
     * LoadingEvent 생성자
     * 
     * @param userId 사용자 ID
     * @param streamId 스트림 ID
     * @param messageContent 전송된 메시지 내용
     */
    public LoadingEvent(Long userId, String streamId, String messageContent) {
        super(userId);
        this.streamId = streamId;
        this.messageContent = messageContent;
    }
}