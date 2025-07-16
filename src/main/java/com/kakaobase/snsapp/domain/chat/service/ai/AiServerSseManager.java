package com.kakaobase.snsapp.domain.chat.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kakaobase.snsapp.domain.chat.dto.ai.response.AiStreamData;
import com.kakaobase.snsapp.domain.chat.exception.StreamException;
import com.kakaobase.snsapp.domain.chat.util.AiServerHealthStatus;
import com.kakaobase.snsapp.domain.chat.exception.ChatException;
import com.kakaobase.snsapp.domain.chat.exception.errorcode.StreamErrorCode;
import com.kakaobase.snsapp.domain.chat.service.communication.ChatWebSocketService;
import com.kakaobase.snsapp.domain.chat.service.streaming.StreamingSessionManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.Disposable;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

/**
 * AI 서버와의 단일 SSE 연결 관리
 */
@Slf4j
@Service
public class AiServerSseManager {
    
    public AiServerSseManager(@Qualifier("webFluxClient") WebClient webFluxClient,
                              @Qualifier("generalWebClient") WebClient generalWebClient,
                              ChatWebSocketService chatWebSocketService,
                              StreamingSessionManager streamingSessionManager,
                              ObjectMapper objectMapper) {
        this.webFluxClient = webFluxClient;
        this.generalWebClient = generalWebClient;
        this.chatWebSocketService = chatWebSocketService;
        this.streamingSessionManager = streamingSessionManager;
        this.objectMapper = objectMapper;
    }

    private final ChatWebSocketService chatWebSocketService;
    @Value("${ai.server.url}")
    private String aiServerUrl;
    
    @Value("${ai.server.health-endpoint:/docs}")
    private String healthEndpoint;
    
    @Qualifier("webFluxClient")
    private final WebClient webFluxClient;
    
    @Qualifier("generalWebClient")
    private final WebClient generalWebClient;
    
    private final StreamingSessionManager streamingSessionManager;
    private final ObjectMapper objectMapper;
    
    private final AtomicReference<AiServerHealthStatus> healthStatus = 
        new AtomicReference<>(AiServerHealthStatus.DISCONNECTED);
    
    private Disposable sseSubscription;
    private LocalDateTime lastHealthCheck;
    private LocalDateTime lastSuccessfulConnection;
    
    /**
     * 애플리케이션 준비 완료 시 SSE 연결 시작
     */
    @EventListener(ApplicationReadyEvent.class)
    public void startSseConnection() {
        log.info("AI 서버 SSE 연결 시작");
        sseConnection();
    }
    
    /**
     * 30초마다 AI 서버 헬스체크 수행
     */
    @Scheduled(fixedRate = 30000)
    public void performHealthCheck() {
        log.info("AI 서버 헬스체크 시작");
        
        try {
            generalWebClient.get()
                    .uri(aiServerUrl + healthEndpoint)
                    .retrieve()
                    .toBodilessEntity()
                    .timeout(Duration.ofSeconds(10))
                    .block();
            
            setHealthStatus(AiServerHealthStatus.CONNECTED);
            lastSuccessfulConnection = LocalDateTime.now();
            log.info("AI 서버 헬스체크 성공");
        } catch (Exception error) {
            setHealthStatus(AiServerHealthStatus.DISCONNECTED);
            log.warn("AI 서버 헬스체크 실패: {}", error.getMessage());
        }
        
        lastHealthCheck = LocalDateTime.now();
    }
    
    /**
     * SSE 연결 수립
     */
    private void sseConnection() {
        log.info("AI 서버 SSE 연결 수립 시도: {}", aiServerUrl);
        
        setHealthStatus(AiServerHealthStatus.CONNECTING);
        
        try {
            sseSubscription = webFluxClient.get()
                    .uri(aiServerUrl + "/chat/stream")
                    .accept(MediaType.TEXT_EVENT_STREAM)
                    .retrieve()
                    .bodyToFlux(ServerSentEvent.class)
                    .doOnSubscribe(subscription -> {
                        log.info("AI 서버 SSE 연결 성공");
                        setHealthStatus(AiServerHealthStatus.CONNECTED);
                    })
                    .doOnError(error -> {
                        log.error("AI 서버 SSE 연결 에러: {}", error.getMessage(), error);
                        setHealthStatus(AiServerHealthStatus.DISCONNECTED);
                        retryConnection();
                    })
                    .subscribe(
                        sse -> processSseEvent(sse),
                        error -> handleConnectionError(error)
                    );
        } catch (Exception e) {
            log.error("SSE 연결 설정 실패: {}", e.getMessage(), e);
            setHealthStatus(AiServerHealthStatus.DISCONNECTED);
            retryConnection();
        }
    }
    
    /**
     * SSE 이벤트 처리
     */
    private void processSseEvent(ServerSentEvent<String> sse) {
        try {
            String event = sse.event() != null ? sse.event() : "stream";
            String jsonData = sse.data();
            
            if (jsonData == null || jsonData.trim().isEmpty()) {
                log.warn("빈 SSE 데이터 수신: event={}", event);
                return;
            }
            
            log.debug("SSE 이벤트 수신: event={}, data={}", event, jsonData);
            
            // JSON → AiStreamData 자동 파싱
            AiStreamData streamData = objectMapper.readValue(jsonData, AiStreamData.class);
            
            // StreamId 유효성 검증
            if (streamData.streamId() == null || streamData.streamId().isBlank()) {
                log.warn("SSE 응답에 StreamId가 없음: userId={}, event={}", streamingSessionManager.getUserIdByStreamId(streamData.streamId()), event);
                throw new StreamException(StreamErrorCode.INVALID_STREAM_ID, streamingSessionManager.getUserIdByStreamId(streamData.streamId()));
            }
            
            // event 타입별 분기 처리
            switch (event) {
                case "stream" -> streamingSessionManager.processStreamData(streamData);
                case "done" -> streamingSessionManager.processStreamComplete(streamData);
                case "error" -> streamingSessionManager.processStreamError(streamData);
                default -> {
                    log.warn("알 수 없는 SSE 이벤트 타입: {}", event);
                    throw new StreamException(StreamErrorCode.AI_SERVER_RESPONSE_PARSE_FAIL,
                            streamingSessionManager.getUserIdByStreamId(streamData.streamId()));
                }
            }
        } catch (StreamException e) {
            chatWebSocketService.sendStreamErrorToUser(e.getUserId(), e.getErrorCode());
        } catch (ChatException e) {
            chatWebSocketService.sendChatErrorToUser(e.getUserId(), e.getErrorCode());
        }
        catch (Exception e) {
            log.error("SSE 데이터 파싱 실패: {}", sse.data(), e);
        }
    }
    
    
    /**
     * SSE 연결 종료
     */
    public void closeSseConnection() {
        log.info("AI 서버 SSE 연결 종료");
        
        if (sseSubscription != null && !sseSubscription.isDisposed()) {
            sseSubscription.dispose();
        }
        
        setHealthStatus(AiServerHealthStatus.DISCONNECTED);
    }
    
    /**
     * 헬스체크 상태 조회
     */
    public AiServerHealthStatus getHealthStatus() {
        return healthStatus.get();
    }
    
    /**
     * 헬스체크 상태 설정 (플래그 역할만)
     */
    private void setHealthStatus(AiServerHealthStatus status) {
        AiServerHealthStatus oldStatus = healthStatus.getAndSet(status);
        
        if (oldStatus != status) {
            log.info("AI 서버 헬스체크 상태 변경: {} -> {}", oldStatus, status);
        }
    }

    
    /**
     * 연결 재시도 로직
     */
    private void retryConnection() {
        log.warn("AI 서버 연결 재시도 시작");
        
        // 기존 연결 정리
        closeSseConnection();
        
        // 5초 후 재연결 시도 (간단한 재시도 로직)
        try {
            Thread.sleep(5000);
            sseConnection();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("연결 재시도 중단");
        }
    }
    
    /**
     * SSE 연결 에러 처리
     */
    private void handleConnectionError(Throwable error) {
        log.error("AI 서버 SSE 연결 에러: {}", error.getMessage(), error);
        
        setHealthStatus(AiServerHealthStatus.DISCONNECTED);
        
        // 에러 타입별 처리
        if (error instanceof java.net.ConnectException) {
            log.warn("AI 서버 연결 실패, 재시도 중...");
        } else if (error instanceof java.util.concurrent.TimeoutException) {
            log.warn("AI 서버 응답 타임아웃, 재시도 중...");
        }
        
        // 재연결 시도
        retryConnection();
    }
}