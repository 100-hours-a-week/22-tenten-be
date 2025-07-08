package com.kakaobase.snsapp.domain.chat.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;
import reactor.util.concurrent.Queues;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 채팅 버퍼 관리자
 * 사용자별 메시지 스트림, 버퍼, 상태를 관리
 */
@Component
@Slf4j
public class ChatBufferManager {

    private final int maxBufferSize;

    // 사용자별 메시지 스트림 (실시간 전송용)
    private final ConcurrentHashMap<Long, Sinks.Many<String>> userMessageSinks;
    
    // 사용자별 메시지 누적 버퍼 (완전한 메시지 조립용)
    private final ConcurrentHashMap<Long, StringBuilder> userMessageBuffers;
    
    // 사용자별 타이핑 상태
    private final ConcurrentHashMap<Long, Boolean> userTypingStates;

    public ChatBufferManager(@Qualifier("maxBufferSize") Integer maxBufferSize) {
        this.maxBufferSize = maxBufferSize;
        this.userMessageSinks = new ConcurrentHashMap<>();
        this.userMessageBuffers = new ConcurrentHashMap<>();
        this.userTypingStates = new ConcurrentHashMap<>();
    }

    /**
     * 사용자의 메시지 Sink 가져오기 (없으면 생성)
     */
    public Sinks.Many<String> getUserMessageSink(Long userId) {
        return userMessageSinks.computeIfAbsent(userId, id -> createNewSink());
    }

    /**
     * 사용자의 메시지 버퍼 가져오기 (없으면 생성)
     */
    public StringBuilder getUserMessageBuffer(Long userId) {
        return userMessageBuffers.computeIfAbsent(userId, id -> new StringBuilder());
    }

    /**
     * 사용자의 타이핑 상태 설정
     */
    public void setUserTypingState(Long userId, boolean isTyping) {
        userTypingStates.put(userId, isTyping);
        log.debug("사용자 {} 타이핑 상태: {}", userId, isTyping);
    }

    /**
     * 사용자의 타이핑 상태 조회
     */
    public boolean getUserTypingState(Long userId) {
        return userTypingStates.getOrDefault(userId, false);
    }

    /**
     * 사용자 메시지 청크 추가 (실시간 전송 + 버퍼 누적)
     */
    public void addMessageChunk(Long userId, String chunk) {
        // 실시간 스트림에 전송
        Sinks.Many<String> sink = getUserMessageSink(userId);
        Sinks.EmitResult result = sink.tryEmitNext(chunk);
        
        if (result.isFailure()) {
            log.warn("사용자 {}의 메시지 청크 전송 실패: {}", userId, result);
        }

        // 버퍼에 누적
        getUserMessageBuffer(userId).append(chunk);
        
        log.debug("사용자 {}에게 청크 추가: '{}'", userId, chunk);
    }

    /**
     * 사용자 메시지 완료 처리
     */
    public String completeUserMessage(Long userId) {
        StringBuilder buffer = userMessageBuffers.get(userId);
        if (buffer == null) {
            return "";
        }

        String completeMessage = buffer.toString();
        
        // 버퍼 초기화
        buffer.setLength(0);
        
        // Sink 완료 신호
        Sinks.Many<String> sink = userMessageSinks.get(userId);
        if (sink != null) {
            sink.tryEmitComplete();
            // 새로운 Sink로 교체 (다음 메시지용)
            userMessageSinks.put(userId, createNewSink());
        }

        log.info("사용자 {} 메시지 완료: '{}'", userId, completeMessage);
        return completeMessage;
    }

    /**
     * 사용자 버퍼 정리
     */
    public void clearUserBuffer(Long userId) {
        userMessageSinks.remove(userId);
        userMessageBuffers.remove(userId);
        userTypingStates.remove(userId);
        log.info("사용자 {} 버퍼 정리 완료", userId);
    }

    /**
     * 모든 사용자 버퍼 정리
     */
    public void clearAllBuffers() {
        userMessageSinks.clear();
        userMessageBuffers.clear();
        userTypingStates.clear();
        log.info("모든 사용자 버퍼 정리 완료");
    }

    /**
     * 현재 활성 사용자 수 조회
     */
    public int getActiveUserCount() {
        return userMessageSinks.size();
    }

    /**
     * 새로운 메시지 Sink 생성
     */
    private Sinks.Many<String> createNewSink() {
        return Sinks.many()
                .multicast()
                .onBackpressureBuffer(Queues.<String>small(), false);
    }
}