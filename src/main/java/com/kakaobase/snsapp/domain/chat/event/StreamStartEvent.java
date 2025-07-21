package com.kakaobase.snsapp.domain.chat.event;

import lombok.Getter;

/**
 * AI 서버로부터 첫 번째 스트림 응답 수신 이벤트
 * 스트리밍 시작을 사용자에게 알리기 위한 이벤트
 */
@Getter
public class StreamStartEvent extends ChatEvent {
    
    /**
     * 스트림 ID
     */
    private final String streamId;
    
    /**
     * 첫 번째 스트림 데이터 내용
     */
    private final String firstStreamData;
    
    /**
     * StreamStartEvent 생성자
     * 
     * @param userId 사용자 ID
     * @param streamId 스트림 ID
     * @param firstStreamData 첫 번째 스트림 데이터 내용
     */
    public StreamStartEvent(Long userId, String streamId, String firstStreamData) {
        super(userId);
        this.streamId = streamId;
        this.firstStreamData = firstStreamData;
    }
}