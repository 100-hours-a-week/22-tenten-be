package com.kakaobase.snsapp.global.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

/**
 * AI 서버 연결 상태 관리자
 * SSE 연결 상태 추적 및 헬스체크 담당
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AiHealthManager {

    @Qualifier("aiServerWebClient")
    private final WebClient aiServerWebClient;
    
    private final SimpMessagingTemplate messagingTemplate;

    @Value("${ai.server.health-endpoint}")
    private String healthEndpoint;

    @Getter
    private final AtomicReference<AiServerStatus> status = new AtomicReference<>(AiServerStatus.UNKNOWN);
    
    @Getter
    private LocalDateTime lastHealthCheck;

    @Getter
    private LocalDateTime lastSuccessfulConnection;

    /**
     * AI 서버 상태 열거형
     */
    public enum AiServerStatus {
        CONNECTED("연결됨"),
        DISCONNECTED("연결 끊김"),
        DEGRADED("성능 저하"),
        UNKNOWN("상태 불명");

        @Getter
        private final String description;

        AiServerStatus(String description) {
            this.description = description;
        }
    }

    /**
     * 30초마다 AI 서버 헬스체크 수행
     */
    @Scheduled(fixedRate = 30000)
    public void performHealthCheck() {
        log.debug("AI 서버 헬스체크 시작");
        
        aiServerWebClient.get()
                .uri(healthEndpoint)
                .retrieve()
                .toBodilessEntity()
                .timeout(Duration.ofSeconds(5))
                .doOnSuccess(response -> {
                    updateStatus(AiServerStatus.CONNECTED);
                    lastSuccessfulConnection = LocalDateTime.now();
                    log.debug("AI 서버 헬스체크 성공");
                })
                .doOnError(error -> {
                    updateStatus(AiServerStatus.DISCONNECTED);
                    log.warn("AI 서버 헬스체크 실패: {}", error.getMessage());
                })
                .onErrorResume(error -> Mono.empty())
                .subscribe();
        
        lastHealthCheck = LocalDateTime.now();
    }

    /**
     * AI 서버 상태 업데이트 및 클라이언트 알림
     */
    public void updateStatus(AiServerStatus newStatus) {
        AiServerStatus oldStatus = status.get();
        
        if (oldStatus != newStatus) {
            status.set(newStatus);
            log.info("AI 서버 상태 변경: {} → {}", oldStatus.getDescription(), newStatus.getDescription());
            
            // 클라이언트에게 상태 변경 알림
            notifyStatusChange(newStatus);
        }
    }

    /**
     * SSE 연결 성공 시 호출
     */
    public void onSseConnected() {
        updateStatus(AiServerStatus.CONNECTED);
        lastSuccessfulConnection = LocalDateTime.now();
    }

    /**
     * SSE 연결 실패 시 호출
     */
    public void onSseDisconnected() {
        updateStatus(AiServerStatus.DISCONNECTED);
    }

    /**
     * 현재 AI 서버 사용 가능 여부
     */
    public boolean isAvailable() {
        return status.get() == AiServerStatus.CONNECTED;
    }

    /**
     * 클라이언트에게 AI 서버 상태 변경 알림
     */
    private void notifyStatusChange(AiServerStatus status) {
        try {
            messagingTemplate.convertAndSend("/topic/ai.status", 
                new AiStatusMessage(status.name(), status.getDescription(), LocalDateTime.now()));
        } catch (Exception e) {
            log.error("AI 서버 상태 알림 전송 실패", e);
        }
    }

    /**
     * AI 상태 메시지 DTO
     */
    public record AiStatusMessage(
        String status,
        String description,
        LocalDateTime timestamp
    ) {}
}