package com.kakaobase.snsapp.domain.chat.model;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

/**
 * 사용자별 AI 스트리밍 세션 관리
 */
@Slf4j
@Getter
public class StreamingSession {
    
    private final Long userId;
    private final StringBuilder responseBuffer;
    private final LocalDateTime startTime;
    
    public StreamingSession(Long userId) {
        this.userId = userId;
        this.responseBuffer = new StringBuilder();
        this.startTime = LocalDateTime.now();
        
        log.debug("새로운 스트리밍 세션 생성: userId={}", userId);
    }
    
    /**
     * AI 응답 데이터 추가
     */
    public synchronized void appendResponse(String response) {
        if (response != null) {
            responseBuffer.append(response);
            log.debug("스트리밍 응답 추가: userId={}, response={}", userId, response);
        }
    }

    
    /**
     * 현재까지 누적된 응답 내용 조회
     */
    public synchronized String getCurrentResponse() {
        return responseBuffer.toString();
    }
    
    /**
     * 최종 응답 내용 조회
     */
    public synchronized String getFinalResponse() {
        return responseBuffer.toString();
    }
    
    
    
    
    /**
     * 세션 지속 시간 조회 (초)
     */
    public long getDurationSeconds() {
        return java.time.Duration.between(startTime, LocalDateTime.now()).toSeconds();
    }
    
}