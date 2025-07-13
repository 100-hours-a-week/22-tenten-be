package com.kakaobase.snsapp.domain.chat.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

/**
 * 채팅 이벤트 추상 클래스
 * 모든 채팅 관련 이벤트의 기본 클래스
 */
@Getter
@RequiredArgsConstructor
public abstract class ChatEvent {
    
    /**
     * 이벤트 대상 사용자 ID
     */
    private final Long userId;
    
    /**
     * 이벤트 발생 시간
     */
    private final LocalDateTime timestamp;
    
    /**
     * 기본 생성자 (현재 시간으로 timestamp 설정)
     */
    protected ChatEvent(Long userId) {
        this.userId = userId;
        this.timestamp = LocalDateTime.now();
    }
}