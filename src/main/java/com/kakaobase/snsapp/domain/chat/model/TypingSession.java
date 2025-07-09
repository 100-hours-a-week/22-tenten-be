package com.kakaobase.snsapp.domain.chat.model;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 사용자별 타이핑 세션 관리 클래스
 * 
 * <p>사용자의 타이핑 상태와 메시지 버퍼를 관리합니다.</p>
 */
@Slf4j
@Getter
public class TypingSession {
    
    private static final int MAX_BUFFER_SIZE = 1000; // 최대 버퍼 크기 (문자 수)
    
    private final Long userId;
    private final StringBuilder messageBuffer;
    private final AtomicReference<LocalDateTime> lastActivity;
    private volatile ScheduledFuture<?> timeoutTask;
    
    public TypingSession(Long userId) {
        this.userId = userId;
        this.messageBuffer = new StringBuilder();
        this.lastActivity = new AtomicReference<>(LocalDateTime.now());
        log.debug("새로운 타이핑 세션 생성: userId={}", userId);
    }
    
    /**
     * 마지막 활동 시간 업데이트
     */
    public void updateActivity() {
        lastActivity.set(LocalDateTime.now());
        log.debug("타이핑 활동 시간 업데이트: userId={}, time={}", userId, lastActivity.get());
    }
    
    /**
     * 메시지 버퍼에 텍스트 추가
     */
    public synchronized void appendMessage(String message) {
        if (message != null && !message.trim().isEmpty()) {
            // 버퍼 크기 제한 확인
            if (messageBuffer.length() + message.length() > MAX_BUFFER_SIZE) {
                log.warn("메시지 버퍼 크기 초과, 기존 버퍼 초기화: userId={}", userId);
                messageBuffer.setLength(0);
            }
            
            // 구분자 추가 (기존 내용이 있는 경우)
            if (messageBuffer.length() > 0) {
                messageBuffer.append(" ");
            }
            
            messageBuffer.append(message.trim());
            updateActivity();
            
            log.debug("메시지 버퍼에 추가: userId={}, message={}, bufferLength={}", 
                userId, message, messageBuffer.length());
        }
    }
    
    /**
     * 버퍼의 모든 내용을 가져오고 초기화
     */
    public synchronized String consumeBuffer() {
        String result = messageBuffer.toString().trim();
        messageBuffer.setLength(0);
        
        log.debug("메시지 버퍼 소비: userId={}, content={}", userId, result);
        return result;
    }
    
    /**
     * 현재 버퍼가 비어있는지 확인
     */
    public synchronized boolean isBufferEmpty() {
        return messageBuffer.length() == 0;
    }
    
    /**
     * 현재 버퍼 내용 확인 (소비하지 않음)
     */
    public synchronized String peekBuffer() {
        return messageBuffer.toString().trim();
    }
    
    /**
     * 타임아웃 태스크 설정
     */
    public void setTimeoutTask(ScheduledFuture<?> task) {
        // 기존 태스크가 있으면 취소
        if (this.timeoutTask != null && !this.timeoutTask.isCancelled()) {
            this.timeoutTask.cancel(false);
            log.debug("기존 타이머 취소: userId={}", userId);
        }
        
        this.timeoutTask = task;
        log.debug("새로운 타이머 설정: userId={}", userId);
    }
    
    /**
     * 세션 정리
     */
    public void cleanup() {
        if (timeoutTask != null && !timeoutTask.isCancelled()) {
            timeoutTask.cancel(false);
        }
        
        synchronized (this) {
            messageBuffer.setLength(0);
        }
        
        log.debug("타이핑 세션 정리 완료: userId={}", userId);
    }
    
    /**
     * 마지막 활동으로부터 경과된 시간(초)
     */
    public long getSecondsSinceLastActivity() {
        return java.time.Duration.between(lastActivity.get(), LocalDateTime.now()).toSeconds();
    }
    
    /**
     * 세션이 활성 상태인지 확인 (1초 이내 활동)
     */
    public boolean isActive() {
        return getSecondsSinceLastActivity() < 1;
    }
}