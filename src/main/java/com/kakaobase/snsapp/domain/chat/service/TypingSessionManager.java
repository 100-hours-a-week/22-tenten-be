package com.kakaobase.snsapp.domain.chat.service;

import com.kakaobase.snsapp.domain.chat.model.TypingSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * 타이핑 세션 전역 관리 서비스
 * 
 * <p>사용자별 타이핑 상태를 추적하고 1초 타이머를 관리합니다.</p>
 */
@Slf4j
@Service
public class TypingSessionManager {
    
    private final ScheduledExecutorService typingScheduler;
    private final ConcurrentHashMap<Long, TypingSession> activeSessions = new ConcurrentHashMap<>();
    
    public TypingSessionManager(@Qualifier("typingScheduler") ScheduledExecutorService typingScheduler) {
        this.typingScheduler = typingScheduler;
    }
    
    /**
     * 타이핑 이벤트 처리
     * 
     * @param userId 사용자 ID
     * @param timeoutCallback 1초 후 실행될 콜백 함수
     */
    public void handleTypingEvent(Long userId, Consumer<TypingSession> timeoutCallback) {
        log.debug("타이핑 이벤트 처리: userId={}", userId);
        
        TypingSession session = activeSessions.computeIfAbsent(userId, TypingSession::new);
        session.updateActivity();
        
        // 새로운 1초 타이머 설정
        scheduleTimeout(session, timeoutCallback);
    }
    
    /**
     * 채팅 메시지 추가
     * 
     * @param userId 사용자 ID
     * @param message 메시지 내용
     * @param timeoutCallback 1초 후 실행될 콜백 함수
     */
    public void addMessage(Long userId, String message, Consumer<TypingSession> timeoutCallback) {
        log.debug("메시지 추가: userId={}, message={}", userId, message);
        
        TypingSession session = activeSessions.computeIfAbsent(userId, TypingSession::new);
        session.appendMessage(message);
        
        // 새로운 1초 타이머 설정
        scheduleTimeout(session, timeoutCallback);
    }
    
    /**
     * 세션 강제 종료 및 버퍼 내용 반환
     */
    public String forceCompleteSession(Long userId) {
        TypingSession session = activeSessions.remove(userId);
        if (session != null) {
            String content = session.consumeBuffer();
            session.cleanup();
            
            log.info("세션 강제 완료: userId={}, content={}", userId, content);
            return content;
        }
        return "";
    }
    
    /**
     * 특정 사용자의 현재 버퍼 내용 확인
     */
    public String peekUserBuffer(Long userId) {
        TypingSession session = activeSessions.get(userId);
        return session != null ? session.peekBuffer() : "";
    }
    
    /**
     * 사용자의 활성 세션이 있는지 확인
     */
    public boolean hasActiveSession(Long userId) {
        TypingSession session = activeSessions.get(userId);
        return session != null && session.isActive();
    }
    
    /**
     * 1초 타이머 스케줄링
     */
    private void scheduleTimeout(TypingSession session, Consumer<TypingSession> timeoutCallback) {
        ScheduledFuture<?> timeoutTask = typingScheduler.schedule(() -> {
            try {
                // 세션이 여전히 활성 상태인지 확인
                if (!session.isActive()) {
                    log.debug("타이머 만료 - 세션 처리: userId={}", session.getUserId());
                    
                    // 세션을 맵에서 제거
                    activeSessions.remove(session.getUserId());
                    
                    // 콜백 실행 (비어있지 않은 경우만)
                    if (!session.isBufferEmpty()) {
                        timeoutCallback.accept(session);
                    }
                    
                    // 세션 정리
                    session.cleanup();
                } else {
                    log.debug("세션이 여전히 활성 상태: userId={}", session.getUserId());
                }
                
            } catch (Exception e) {
                log.error("타이머 콜백 실행 중 오류: userId={}, error={}", 
                    session.getUserId(), e.getMessage(), e);
            }
        }, 1, TimeUnit.SECONDS);
        
        session.setTimeoutTask(timeoutTask);
    }
    
    /**
     * 주기적으로 비활성 세션 정리 (5분마다)
     */
    @Scheduled(fixedRate = 300000) // 5분
    public void cleanupInactiveSessions() {
        log.debug("비활성 세션 정리 시작, 현재 세션 수: {}", activeSessions.size());
        
        activeSessions.entrySet().removeIf(entry -> {
            TypingSession session = entry.getValue();
            
            // 5분 이상 비활성 세션 제거
            if (session.getSecondsSinceLastActivity() > 300) {
                log.info("비활성 세션 제거: userId={}, lastActivity={}초 전", 
                    entry.getKey(), session.getSecondsSinceLastActivity());
                
                session.cleanup();
                return true;
            }
            return false;
        });
        
        log.debug("비활성 세션 정리 완료, 남은 세션 수: {}", activeSessions.size());
    }
    
    /**
     * 모든 세션 강제 종료 (애플리케이션 종료 시)
     */
    public void shutdownAllSessions() {
        log.info("모든 타이핑 세션 종료 시작: {}", activeSessions.size());
        
        activeSessions.values().forEach(TypingSession::cleanup);
        activeSessions.clear();
        
        log.info("모든 타이핑 세션 종료 완료");
    }
    
    /**
     * 현재 활성 세션 수 반환
     */
    public int getActiveSessionCount() {
        return activeSessions.size();
    }
}