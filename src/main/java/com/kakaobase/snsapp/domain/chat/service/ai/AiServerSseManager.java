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
import org.springframework.web.reactive.function.client.WebClientResponseException;
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
     * 30초마다 조건부 헬스체크 및 재연결 수행
     * DISCONNECTED 상태일 때만 실행
     */
    @Scheduled(fixedRate = 30000)
    public void performHealthCheck() {
        if (healthStatus.get() == AiServerHealthStatus.DISCONNECTED) {
            log.info("연결 끊김 상태, 헬스체크 및 재연결 시도");
            performHealthCheckAndReconnect();
        } else {
            log.debug("연결 유지 중 ({}), 헬스체크 스킵", healthStatus.get());
        }
        
        lastHealthCheck = LocalDateTime.now();
    }
    
    /**
     * 헬스체크 수행 후 성공 시 SSE 재연결 시도
     */
    private void performHealthCheckAndReconnect() {
        try {
            generalWebClient.get()
                    .uri(aiServerUrl + healthEndpoint)
                    .retrieve()
                    .toBodilessEntity()
                    .timeout(Duration.ofSeconds(10))
                    .block();
            
            log.info("헬스체크 성공, SSE 재연결 시도");
            lastSuccessfulConnection = LocalDateTime.now();
            
            // SSE 재연결 시도
            setHealthStatus(AiServerHealthStatus.CONNECTING);
            sseConnection();
            
        } catch (Exception error) {
            log.warn("헬스체크 실패, 30초 후 재시도: {}", error.getMessage());
            // DISCONNECTED 상태 유지하여 다음 주기에 재시도
            setHealthStatus(AiServerHealthStatus.DISCONNECTED);
        }
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
                        // 연결 성공 후 ReadTimeout은 정상 동작
                        if (error instanceof java.util.concurrent.TimeoutException && 
                            healthStatus.get() == AiServerHealthStatus.CONNECTED) {
                            log.debug("SSE 연결 유지 중 Timeout (정상): {}", error.getMessage());
                        } else if (isServerDownError(error)) {
                            log.error("AI 서버 다운 감지, SSE 연결 종료: {}", error.getMessage());
                            setHealthStatus(AiServerHealthStatus.DISCONNECTED);
                            closeSseConnection();
                        } else {
                            log.warn("SSE 에러 발생하지만 연결 유지: {}", error.getMessage());
                        }
                    })
                    .subscribe(
                        sse -> processSseEvent(sse),
                        error -> {
                            // 연결 성공 후 ReadTimeout은 정상 동작
                            if (error instanceof java.util.concurrent.TimeoutException && 
                                healthStatus.get() == AiServerHealthStatus.CONNECTED) {
                                log.debug("SSE 구독 중 Timeout (정상): {}", error.getMessage());
                            } else {
                                handleConnectionError(error);
                            }
                        }
                    );
        } catch (Exception e) {
            log.error("SSE 연결 설정 실패: {}", e.getMessage(), e);
            handleConnectionError(e);
        }
    }
    
    /**
     * SSE 이벤트 처리
     */
    private void processSseEvent(ServerSentEvent<String> sse) {
        try {
            String event = sse.event() != null ? sse.event() : "stream";
            
            // 데이터 타입 확인 로그 추가
            Object rawData = sse.data();
            log.info("SSE 데이터 타입 확인: type={}, data={}", rawData != null ? rawData.getClass().getName() : "null", rawData);
            
            if (rawData == null) {
                log.warn("빈 SSE 데이터 수신: event={}", event);
                return;
            }
            
            // 연결 확인 메시지 처리
            if (isConnectionMessage(rawData)) {
                handleConnectionMessage(rawData);
                return;
            }
            
            log.debug("SSE 이벤트 수신: event={}, data={}", event, rawData);
            
            // 스트림 데이터 파싱
            AiStreamData streamData = parseToAiStreamData(rawData);
            
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
     * 연결 확인 메시지인지 판단
     */
    private boolean isConnectionMessage(Object rawData) {
        if (rawData instanceof java.util.Map<?, ?> map) {
            // message만 있고 stream_id가 없으면 연결 확인 메시지
            return map.containsKey("message") && !map.containsKey("stream_id");
        }
        return false;
    }
    
    /**
     * 연결 확인 메시지 처리
     */
    private void handleConnectionMessage(Object rawData) {
        if (rawData instanceof java.util.Map<?, ?> map) {
            String message = (String) map.get("message");
            log.info("SSE 연결 확인 메시지 수신: {}", message);
            
            // 연결이 완료되었음을 확인
            if (healthStatus.get() != AiServerHealthStatus.CONNECTED) {
                setHealthStatus(AiServerHealthStatus.CONNECTED);
            }
        }
    }
    
    /**
     * SSE 데이터를 AiStreamData로 파싱 (String과 Object 모두 지원)
     */
    private AiStreamData parseToAiStreamData(Object rawData) {
        try {
            if (rawData == null) {
                throw new IllegalArgumentException("SSE 데이터가 null입니다");
            }
            
            // String 타입인 경우 - JSON 파싱
            if (rawData instanceof String jsonString) {
                log.debug("JSON 문자열 파싱: {}", jsonString);
                return objectMapper.readValue(jsonString, AiStreamData.class);
            }
            
            // 이미 파싱된 객체인 경우 - 직접 변환
            if (rawData instanceof java.util.Map<?, ?> dataMap) {
                log.debug("Map 객체 직접 변환: {}", dataMap);
                return objectMapper.convertValue(dataMap, AiStreamData.class);
            }
            
            // 기타 타입인 경우 - 일반 객체 변환 시도
            log.debug("기타 타입 변환 시도: type={}, data={}", rawData.getClass().getName(), rawData);
            return objectMapper.convertValue(rawData, AiStreamData.class);
            
        } catch (Exception e) {
            log.error("SSE 데이터 파싱 실패: type={}, data={}, error={}", 
                rawData.getClass().getName(), rawData, e.getMessage(), e);
            throw new RuntimeException("SSE 데이터 파싱 실패", e);
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
     * 서버 다운 에러 판별 (502, 503, 504, ConnectException만 서버 다운으로 처리)
     */
    private boolean isServerDownError(Throwable error) {
        // ConnectException: 서버 접근 불가
        if (error instanceof java.net.ConnectException) {
            return true;
        }
        
        // WebClientResponseException: HTTP 에러 코드 확인
        if (error instanceof WebClientResponseException webClientError) {
            int statusCode = webClientError.getStatusCode().value();
            return statusCode == 502 || statusCode == 503 || statusCode == 504;
        }
        
        return false;
    }
    
    /**
     * SSE 연결 에러 처리
     */
    private void handleConnectionError(Throwable error) {
        if (isServerDownError(error)) {
            log.error("AI 서버 다운으로 인한 연결 에러: {}", error.getMessage());
            setHealthStatus(AiServerHealthStatus.DISCONNECTED);
            closeSseConnection();
            // 즉시 재시도 없음, 30초 후 스케줄러가 처리
        } else {
            log.warn("일시적 연결 에러, 연결 유지: {}", error.getMessage());
            // 연결 유지, 즉시 재시도 하지 않음
        }
    }
}