package com.kakaobase.snsapp.domain.chat.service;

import com.kakaobase.snsapp.domain.chat.dto.ai.request.ChatBlockData;
import com.kakaobase.snsapp.domain.chat.dto.ai.response.AiServerResponse;
import com.kakaobase.snsapp.domain.chat.exception.ChatException;
import com.kakaobase.snsapp.domain.chat.exception.errorcode.ChatErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.io.IOException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

/**
 * AI 서버 HTTP 통신 전용 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiServerHttpClient {
    
    @Qualifier("webFluxClient")
    private final WebClient webClient;
    
    private final StreamingSessionManager streamingSessionManager;
    private final ChatCommandService chatCommandService;
    
    @Value("${ai.server.url}")
    private String aiServerUrl;
    
    @Value("${ai.server.chat.endpoint:/api/ai/chat}")
    private String aiChatEndpoint;
    
    // 타임아웃 설정
    private static final Duration CONNECTION_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration WRITE_TIMEOUT = Duration.ofSeconds(5);
    
    /**
     * AI 서버로 채팅 블록 데이터 비동기 전송 (공개 메서드)
     */
    public void sendChatBlock(ChatBlockData chatBlockData) {
        Long userId = streamingSessionManager.getUserIdByStreamId(chatBlockData.streamId());
        log.info("AI 서버로 채팅 블록 비동기 전송 시작: streamId={}, userId={}", 
            chatBlockData.streamId(), userId);
        
        sendChatBlockAsync(chatBlockData)
            .subscribe(
                response -> {
                    log.info("AI 서버 StreamQueue 등록 성공: streamId={}, message={}", 
                        chatBlockData.streamId(), response.message());
                },
                error -> {
                    log.error("AI 서버 채팅 블록 전송 최종 실패: streamId={}, userId={}, error={}", 
                        chatBlockData.streamId(), userId, error.getMessage(), error);
                    
                    // 사용자에게 에러 알림
                    if (userId != null && userId > 0) {
                        ChatErrorCode errorCode = determineErrorCode(error);
                        chatCommandService.sendErrorToUser(userId, errorCode);
                    }
                }
            );
    }
    
    /**
     * AI 서버로 채팅 블록 데이터 비동기 전송 (내부 구현)
     */
    private Mono<AiServerResponse> sendChatBlockAsync(ChatBlockData chatBlockData) {
        return webClient.post()
            .uri(aiServerUrl + aiChatEndpoint)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(chatBlockData)
            .retrieve()
            .onStatus(
                status -> status.is4xxClientError(),
                clientResponse -> clientResponse.bodyToMono(AiServerResponse.class)
                    .map(response -> new ChatException(ChatErrorCode.AI_SERVER_CLIENT_ERROR, 
                        streamingSessionManager.getUserIdByStreamId(chatBlockData.streamId())))
            )
            .onStatus(
                status -> status.is5xxServerError(),
                clientResponse -> clientResponse.bodyToMono(AiServerResponse.class)
                    .map(response -> new ChatException(ChatErrorCode.AI_SERVER_INTERNAL_ERROR, 
                        streamingSessionManager.getUserIdByStreamId(chatBlockData.streamId())))
            )
            .bodyToMono(AiServerResponse.class)
            .timeout(READ_TIMEOUT)
            .retryWhen(
                Retry.fixedDelay(2, Duration.ofSeconds(1))
                    .filter(this::isRetryableError)
                    .doBeforeRetry(retrySignal -> {
                        log.warn("AI 서버 요청 재시도: streamId={}, attempt={}, error={}", 
                            chatBlockData.streamId(), retrySignal.totalRetries() + 1, 
                            retrySignal.failure().getMessage());
                    })
            )
            .doOnNext(response -> {
                if (response.isStreamQueueRegistered()) {
                    log.info("AI 서버 StreamQueue 등록 성공: streamId={}, message={}", 
                        chatBlockData.streamId(), response.message());
                } else {
                    log.warn("AI 서버 응답 이상: streamId={}, message={}, error={}", 
                        chatBlockData.streamId(), response.message(), response.error());
                }
            });
    }
    
    /**
     * 재시도 가능한 에러인지 판단
     */
    private boolean isRetryableError(Throwable error) {
        return error instanceof ConnectException ||
               error instanceof TimeoutException ||
               error instanceof IOException ||
               error instanceof UnknownHostException ||
               (error instanceof ChatException && 
                ((ChatException) error).getErrorCode() == ChatErrorCode.AI_SERVER_INTERNAL_ERROR);
    }
    
    /**
     * 에러 타입에 따른 적절한 ChatErrorCode 결정
     */
    private ChatErrorCode determineErrorCode(Throwable error) {
        if (error instanceof ConnectException || error instanceof UnknownHostException) {
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