package com.kakaobase.snsapp.domain.chat.service.streaming;

import com.kakaobase.snsapp.domain.chat.converter.ChatConverter;
import com.kakaobase.snsapp.domain.chat.dto.ai.request.ChatBlockData;
import com.kakaobase.snsapp.domain.chat.event.LoadingEvent;
import com.kakaobase.snsapp.domain.chat.exception.errorcode.ChatErrorCode;
import com.kakaobase.snsapp.domain.chat.service.ai.AiServerHttpClient;
import com.kakaobase.snsapp.domain.chat.service.communication.ChatWebSocketService;
import com.kakaobase.snsapp.domain.chat.util.ChatBufferCacheUtil;
import com.kakaobase.snsapp.domain.members.entity.Member;
import com.kakaobase.snsapp.domain.members.repository.MemberRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
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
public class ChatBufferManager {
    
    private final ApplicationEventPublisher eventPublisher;
    private final ChatBufferCacheUtil cacheUtil;
    private final StreamingSessionManager streamingSessionManager;
    private final MemberRepository memberRepository;
    private final ChatWebSocketService chatWebSocketService;
    private final AiServerHttpClient aiServerHttpClient;
    private final ChatConverter chatConverter;
    
    private ScheduledExecutorService scheduler;
    private final ConcurrentHashMap<Long, ScheduledFuture<?>> userTimers = new ConcurrentHashMap<>();
    
    // 타이머 설정
    @Value("${chat.buffer.loading-delay:3}")
    private int loadingDelaySeconds; // 로딩 패킷 전송 지연 시간 (초)
    
    @Value("${chat.buffer.send-delay:1}")
    private int sendDelaySeconds; // AI 서버 요청 지연 시간 (초)
    
    @Value("${chat.buffer.shutdown-timeout:5}")
    private int shutdownTimeoutSeconds; // 스케줄러 종료 대기 시간 (초)
    
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
            if (!scheduler.awaitTermination(shutdownTimeoutSeconds, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        log.info("ChatTimerManager 종료 완료");
    }
    
    
    /**
     * 사용자 타이머 취소
     */
    public void cancelTimer(Long userId) {
        log.info("타이머 취소 시작: userId={}, 현재 활성 타이머 수={}", userId, userTimers.size());
        
        ScheduledFuture<?> timer = userTimers.remove(userId);
        if (timer != null && !timer.isCancelled()) {
            timer.cancel(false);
            log.info("타이머 취소 완료: userId={}, 남은 활성 타이머 수={}", userId, userTimers.size());
        } else {
            log.info("취소할 타이머 없음: userId={}", userId);
        }
    }
    
    /**
     * 3초 대기 후 로딩 패킷 전송 + 1초 후 버퍼 전송 스케줄링
     */
    public void resetTimerWithDelayedLoading(Long userId) {
        log.info("지연 로딩 타이머 리셋 시작: userId={}, 현재 활성 타이머 수={}", userId, userTimers.size());
        
        try {
            // 1. 기존 타이머 취소
            ScheduledFuture<?> oldTimer = userTimers.get(userId);
            if (oldTimer != null && !oldTimer.isCancelled()) {
                oldTimer.cancel(false);
                log.info("기존 타이머 취소 완료: userId={}", userId);
            }
            
            // 2. 3초 후 로딩 패킷 전송 및 AI 서버 요청 스케줄링
            ScheduledFuture<?> newTimer = scheduler.schedule(
                () -> triggerLoadingAndScheduleAiRequest(userId), 
                loadingDelaySeconds, 
                TimeUnit.SECONDS
            );
            
            userTimers.put(userId, newTimer);
            log.info("지연 로딩 타이머 리셋 완료: userId={}, 3초 후 로딩 패킷 전송 예정, 총 활성 타이머 수={}", 
                userId, userTimers.size());
            
        } catch (Exception e) {
            log.error("지연 로딩 타이머 리셋 실패: userId={}, error={}", userId, e.getMessage(), e);
        }
    }
    
    /**
     * 3초 후 로딩 패킷 전송 및 1초 후 AI 서버 요청 스케줄링
     */
    private void triggerLoadingAndScheduleAiRequest(Long userId) {
        log.info("로딩 패킷 전송 및 AI 서버 요청 스케줄링 시작: userId={}, 실행 시간={}", 
            userId, System.currentTimeMillis());
        
        try {
            // 1. 버퍼 확인 및 준비 작업
            if (!cacheUtil.hasBuffer(userId)) {
                log.info("버퍼가 없어서 로딩 패킷 전송 안함: userId={}", userId);
                chatWebSocketService.sendChatErrorToUser(userId, ChatErrorCode.CHAT_BUFFER_NOT_FOUND);
                return;
            }
            
            // 2. 버퍼 내용 미리 추출 (중복 검증 제거)
            String bufferContent = cacheUtil.getAndDeleteBuffer(userId);
            if (bufferContent == null || bufferContent.isBlank()) {
                log.info("버퍼 내용이 비어서 로딩 패킷 전송 안함: userId={}", userId);
                chatWebSocketService.sendChatErrorToUser(userId, ChatErrorCode.CHAT_BUFFER_INVALID);
                return;
            }
            log.info("버퍼 내용 추출 완료: userId={}, contentLength={}", userId, bufferContent.length());
            
            // 3. 사용자 정보 조회
            Member member = memberRepository.findById(userId).orElse(null);
            if (member == null) {
                log.warn("사용자 정보 없음: userId={}", userId);
                chatWebSocketService.sendChatErrorToUser(userId, ChatErrorCode.USER_NOT_FOUND);
                return;
            }
            
            // 4. 스트리밍 세션 시작
            String streamId = streamingSessionManager.startStreaming(userId);
            
            // 5. 즉시 로딩 패킷 전송
            log.info("LoadingEvent 발행: userId={}, streamId={}", userId, streamId);
            eventPublisher.publishEvent(new LoadingEvent(userId, streamId, bufferContent));
            
            // 6. 1초 후 버퍼 전송 스케줄링 (버퍼 내용과 사용자 정보 전달)
            ScheduledFuture<?> aiRequestTimer = scheduler.schedule(
                () -> triggerBufferSendSafely(userId, bufferContent, member, streamId), 
                sendDelaySeconds, 
                TimeUnit.SECONDS
            );
            
            userTimers.put(userId, aiRequestTimer);
            log.info("로딩 패킷 전송 완료 및 AI 서버 요청 스케줄링 완료: userId={}, streamId={}, 1초 후 AI 서버 요청 예정", 
                userId, streamId);
            
        } catch (Exception e) {
            log.error("로딩 패킷 전송 및 AI 서버 요청 스케줄링 실패: userId={}, error={}", userId, e.getMessage(), e);
            // 타이머에서 실행되므로 예외를 먹어서 스레드가 죽지 않도록 함
        } finally {
            // 첫 번째 타이머 완료 후 정리 (두 번째 타이머는 userTimers에 새로 등록됨)
            log.debug("첫 번째 타이머 정리 완료: userId={}", userId);
        }
    }
    
    
    /**
     * 안전한 버퍼 전송 트리거 (예외 처리 포함)
     */
    private void triggerBufferSendSafely(Long userId, String bufferContent, Member member, String streamId) {
        log.info("타이머 트리거 실행 시작: userId={}, streamId={}, 실행 시간={}", 
            userId, streamId, System.currentTimeMillis());
        
        try {
            sendBufferToAiServerDirectly(userId, bufferContent, member, streamId);
            log.info("타이머 트리거 버퍼 전송 완료: userId={}, streamId={}", userId, streamId);
        } catch (Exception e) {
            log.error("타이머 트리거 버퍼 전송 실패: userId={}, streamId={}, error={}", 
                userId, streamId, e.getMessage(), e);
            // 타이머에서 실행되므로 예외를 먹어서 스레드가 죽지 않도록 함
        } finally {
            // 타이머 완료 후 정리
            userTimers.remove(userId);
            log.info("타이머 정리 완료: userId={}, 남은 활성 타이머 수={}", userId, userTimers.size());
        }
    }
    
    
    /**
     * 채팅 버퍼를 AI 서버로 직접 전송
     */
    private void sendBufferToAiServerDirectly(Long userId, String bufferContent, Member member, String streamId) {
        log.info("AI 서버 자동 전송 시작: userId={}, streamId={}, 시작 시간={}", 
            userId, streamId, System.currentTimeMillis());
        
        // ChatBlockData 생성
        ChatBlockData chatBlockData = chatConverter.toChatBlockData(streamId, bufferContent, member);
        log.info("ChatBlockData 생성 완료: userId={}, streamId={}", userId, streamId);
        
        // AI 서버로 HTTP 전송
        aiServerHttpClient.sendChatBlock(chatBlockData);
        log.info("AI 서버 HTTP 전송 완료: userId={}, streamId={}", userId, streamId);
        
        log.info("AI 서버 자동 전송 완료: userId={}, streamId={}, contentLength={}, 완료 시간={}", 
            userId, streamId, bufferContent.length(), System.currentTimeMillis());
    }
    
}