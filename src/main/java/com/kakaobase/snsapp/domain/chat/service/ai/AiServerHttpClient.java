package com.kakaobase.snsapp.domain.chat.service.ai;

import com.kakaobase.snsapp.domain.chat.dto.ai.request.ChatBlockData;
import com.kakaobase.snsapp.domain.chat.dto.ai.response.AiServerResponse;
import com.kakaobase.snsapp.domain.chat.exception.ChatException;
import com.kakaobase.snsapp.domain.chat.exception.errorcode.ChatErrorCode;
import com.kakaobase.snsapp.domain.chat.service.streaming.StreamingSessionManager;
import com.kakaobase.snsapp.domain.chat.event.ChatErrorEvent;
import com.kakaobase.snsapp.global.config.WebClientConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;

import java.io.IOException;
import java.net.ConnectException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

/**
 * AI 서버 HTTP 통신 전용 서비스
 */
@Slf4j
@Service
public class AiServerHttpClient {
    
    @Qualifier("generalWebClient")
    private final WebClient webClient;

    private final StreamingSessionManager streamingSessionManager;
    private final ApplicationEventPublisher eventPublisher;
    
    @Value("${ai.server.url}")
    private String aiServerUrl;
    
    @Value("${ai.server.chat.endpoint:/api/ai/chat}")
    private String aiChatEndpoint;
    
    // 타임아웃 설정
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(10);

    public AiServerHttpClient(@Qualifier("generalWebClient") WebClient webClient,
                              StreamingSessionManager streamingSessionManager,
                              ApplicationEventPublisher eventPublisher) {
        this.webClient = webClient;
        this.streamingSessionManager = streamingSessionManager;
        this.eventPublisher = eventPublisher;
    }
    
    /**
     * AI 서버로 채팅 블록 데이터 동기 전송 (공개 메서드)
     */
    public void sendChatBlock(ChatBlockData chatBlockData) {
        Long userId = streamingSessionManager.getUserIdByStreamId(chatBlockData.streamId());
        log.info("AI 서버로 채팅 블록 전송 시작: streamId={}, userId={}", 
            chatBlockData.streamId(), userId);
        
        try {
            AiServerResponse response = sendAiServerRequestSync(HttpMethod.POST, aiChatEndpoint, chatBlockData, chatBlockData.streamId());
            log.info("AI 서버 StreamQueue 등록 성공: streamId={}, message={}", 
                chatBlockData.streamId(), response.message());
        } catch (Exception e) {
            handleError(chatBlockData.streamId(), userId, e);
        }
    }
    
    /**
     * AI 서버로 스트리밍 중지 요청 전송 (공개 메서드)
     */
    public void stopStream(String streamId) {
        Long userId = streamingSessionManager.getUserIdByStreamId(streamId);
        log.info("AI 서버로 스트리밍 중지 요청 시작: streamId={}, userId={}", streamId, userId);
        
        try {
            String endpoint = "/chat/stream/" + streamId;
            AiServerResponse response = sendAiServerRequestSync(HttpMethod.DELETE, endpoint, null, streamId);
            log.info("AI 서버 스트리밍 중지 성공: streamId={}, userId={}, message={}", 
                streamId, userId, response.message());
        } catch (Exception e) {
            handleError(streamId, userId, e);
        }
    }
    
    /**
     * AI 서버로 범용 HTTP 요청 전송 (동기 방식)
     */
    private <T> AiServerResponse sendAiServerRequestSync(HttpMethod method, String endpoint, T body, String requestId) {
        WebClient.RequestBodySpec requestSpec = webClient.method(method)
            .uri(aiServerUrl + endpoint)
            .contentType(MediaType.APPLICATION_JSON);
            
        // body가 있는 경우에만 bodyValue 설정
        WebClient.RequestHeadersSpec<?> headersSpec;
        if (body != null) {
            headersSpec = requestSpec.bodyValue(body);
        } else {
            headersSpec = requestSpec;
        }
        
        try {
            return headersSpec
                .retrieve()
                .onStatus(
                    HttpStatusCode::is4xxClientError,
                    clientResponse -> clientResponse.bodyToMono(AiServerResponse.class)
                        .map(response -> new ChatException(ChatErrorCode.AI_SERVER_CLIENT_ERROR, null))
                )
                .onStatus(
                    HttpStatusCode::is5xxServerError,
                    clientResponse -> clientResponse.bodyToMono(AiServerResponse.class)
                        .map(response -> new ChatException(ChatErrorCode.AI_SERVER_INTERNAL_ERROR, null))
                )
                .bodyToMono(AiServerResponse.class)
                .timeout(READ_TIMEOUT)
                .retryWhen(
                    Retry.fixedDelay(2, Duration.ofSeconds(1))
                        .filter(this::isRetryableError)
                        .doBeforeRetry(retrySignal -> 
                            log.warn("AI 서버 요청 재시도: requestId={}, method={}, endpoint={}, attempt={}, error={}", 
                                requestId, method, endpoint, retrySignal.totalRetries() + 1, 
                                retrySignal.failure().getMessage())
                        )
                )
                .doOnNext(response -> 
                    log.info("AI 서버 요청 성공: requestId={}, method={}, endpoint={}, message={}", 
                        requestId, method, endpoint, response.message())
                )
                .block(); // 동기 처리
        } catch (Exception e) {
            log.error("AI 서버 요청 실패: requestId={}, method={}, endpoint={}, error={}", 
                requestId, method, endpoint, e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * 통합 에러 처리
     */
    private void handleError(String requestId, Long userId, Exception error) {
        log.error("AI 서버 요청 실패: requestId={}, userId={}, error={}", 
            requestId, userId, error.getMessage(), error);
        
        ChatErrorCode errorCode = determineErrorCode(error);
        eventPublisher.publishEvent(new ChatErrorEvent(userId, errorCode, error.getMessage(), requestId));
    }
    
    /**
     * 재시도 가능한 에러인지 판단
     */
    private boolean isRetryableError(Throwable error) {
        return error instanceof IOException ||
               error instanceof TimeoutException ||
               (error instanceof ChatException chatException && 
                chatException.getErrorCode() == ChatErrorCode.AI_SERVER_INTERNAL_ERROR);
    }
    
    /**
     * 에러 타입에 따른 적절한 ChatErrorCode 결정
     */
    private ChatErrorCode determineErrorCode(Throwable error) {
        if (error instanceof ConnectException) {
            return ChatErrorCode.AI_SERVER_CONNECTION_FAIL;
        }
        if (error instanceof TimeoutException) {
            return ChatErrorCode.AI_SERVER_TIMEOUT;
        }
        if (error instanceof ChatException chatException) {
            return chatException.getErrorCode();
        }
        return ChatErrorCode.AI_SERVER_CONNECTION_FAIL;
    }
    
}