package com.kakaobase.snsapp.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.time.Duration;
import java.util.concurrent.Executor;

/**
 * 채팅 기능 전용 설정
 * 타임아웃, 스레드풀 등 채팅 관련 설정들
 */
@Configuration
@EnableAsync
public class ChatConfig {

    @Value("${chat.message.timeout:10}")
    private int messageTimeoutSeconds;

    @Value("${chat.typing.inactivity-threshold:1}")
    private int typingInactivitySeconds;

    @Value("${chat.buffer.max-size:1000}")
    private int maxBufferSize;

    @Value("${chat.connection.retry-attempts:3}")
    private int retryAttempts;

    @Value("${chat.connection.retry-delay:2}")
    private int retryDelaySeconds;

    /**
     * 채팅 메시지 타임아웃 설정
     */
    @Bean("chatMessageTimeout")
    public Duration chatMessageTimeout() {
        return Duration.ofSeconds(messageTimeoutSeconds);
    }

    /**
     * 타이핑 비활성 임계값
     */
    @Bean("typingInactivityThreshold")
    public Duration typingInactivityThreshold() {
        return Duration.ofSeconds(typingInactivitySeconds);
    }

    /**
     * 메시지 버퍼 최대 크기
     */
    @Bean("maxBufferSize")
    public Integer maxBufferSize() {
        return maxBufferSize;
    }

    /**
     * SSE 재연결 설정
     */
    @Bean("sseRetryAttempts")
    public Integer sseRetryAttempts() {
        return retryAttempts;
    }

    @Bean("sseRetryDelay")
    public Duration sseRetryDelay() {
        return Duration.ofSeconds(retryDelaySeconds);
    }

    /**
     * 채팅 메시지 비동기 처리용 ThreadPool
     */
    @Bean("chatAsyncExecutor")
    public Executor chatAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("chat-async-");
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    /**
     * AI 응답 스트리밍 처리용 ThreadPool
     */
    @Bean("aiStreamExecutor")
    public Executor aiStreamExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("ai-stream-");
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}