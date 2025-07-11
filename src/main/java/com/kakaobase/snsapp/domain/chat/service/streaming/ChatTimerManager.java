package com.kakaobase.snsapp.domain.chat.service.streaming;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 채팅 타이머 전용 관리 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatTimerManager {
    
    private final ChatBufferManager chatBufferManager;
    
    private ScheduledExecutorService scheduler;
    private final ConcurrentHashMap<Long, ScheduledFuture<?>> userTimers = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void init() {
        // CPU 코어 수 기반으로 타이머 풀 크기 설정
        int poolSize = Math.max(2, Runtime.getRuntime().availableProcessors() / 2);
        this.scheduler = Executors.newScheduledThreadPool(poolSize, r -> {
            Thread t = new Thread(r, "ChatTimer");
            t.setDaemon(true);
            return t;
        });
        log.info("ChatTimerManager 초기화 완료: 타이머 풀 크기={}", poolSize);
    }
    
    @PreDestroy
    public void destroy() {
        log.info("ChatTimerManager 종료 시작: 활성 타이머 수={}", userTimers.size());
        
        // 모든 타이머 취소
        userTimers.values().forEach(timer -> timer.cancel(false));
        userTimers.clear();
        
        // 스케줄러 종료
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        log.info("ChatTimerManager 종료 완료");
    }
    
    /**
     * 사용자별 타이머 리셋 (1초 후 자동 전송)
     */
    public void resetTimer(Long userId) {
        log.debug("타이머 리셋: userId={}", userId);
        
        // 기존 타이머 취소
        ScheduledFuture<?> oldTimer = userTimers.get(userId);
        if (oldTimer != null && !oldTimer.isCancelled()) {
            oldTimer.cancel(false);
            log.debug("기존 타이머 취소: userId={}", userId);
        }
        
        // 새 타이머 시작 (1초 후 실행)
        ScheduledFuture<?> newTimer = scheduler.schedule(
            () -> triggerBufferSendSafely(userId), 
            1, 
            TimeUnit.SECONDS
        );
        
        userTimers.put(userId, newTimer);
        log.debug("새 타이머 시작: userId={}, 1초 후 실행", userId);
    }
    
    /**
     * 사용자 타이머 취소
     */
    public void cancelTimer(Long userId) {
        log.debug("타이머 취소: userId={}", userId);
        
        ScheduledFuture<?> timer = userTimers.remove(userId);
        if (timer != null && !timer.isCancelled()) {
            timer.cancel(false);
            log.debug("타이머 취소 완료: userId={}", userId);
        }
    }
    
    /**
     * 안전한 버퍼 전송 트리거 (예외 처리 포함)
     */
    private void triggerBufferSendSafely(Long userId) {
        try {
            chatBufferManager.sendBufferToAiServer(userId);
        } catch (Exception e) {
            log.error("타이머 트리거 버퍼 전송 실패: userId={}, error={}", userId, e.getMessage(), e);
            // 타이머에서 실행되므로 예외를 먹어서 스레드가 죽지 않도록 함
        } finally {
            // 타이머 완료 후 정리
            userTimers.remove(userId);
        }
    }
}