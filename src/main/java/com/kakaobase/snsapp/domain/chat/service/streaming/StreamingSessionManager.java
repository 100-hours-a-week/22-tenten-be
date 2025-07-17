package com.kakaobase.snsapp.domain.chat.service.streaming;

import com.kakaobase.snsapp.domain.chat.converter.ChatConverter;
import com.kakaobase.snsapp.domain.chat.dto.response.StreamEndData;
import com.kakaobase.snsapp.domain.chat.event.StreamStartEvent;
import com.kakaobase.snsapp.domain.chat.exception.errorcode.StreamErrorCode;
import com.kakaobase.snsapp.domain.chat.model.StreamingSession;
import com.kakaobase.snsapp.domain.chat.dto.ai.response.AiStreamData;
import com.kakaobase.snsapp.domain.chat.service.communication.ChatCommandService;
import com.kakaobase.snsapp.domain.chat.service.communication.ChatWebSocketService;
import com.kakaobase.snsapp.domain.chat.util.ChatEventType;
import com.kakaobase.snsapp.domain.chat.exception.ChatException;
import com.kakaobase.snsapp.domain.chat.exception.errorcode.ChatErrorCode;
import com.kakaobase.snsapp.domain.chat.util.StreamIdGenerator;
import com.kakaobase.snsapp.domain.chat.exception.StreamException;
import com.kakaobase.snsapp.global.common.entity.WebSocketPacket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 사용자별 스트리밍 세션 관리 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StreamingSessionManager {

    private final StreamIdGenerator streamIdGenerator;
    
    // StreamId 기반 세션 관리
    private final ConcurrentHashMap<String, StreamingSession> activeSessions = new ConcurrentHashMap<>();
    
    private final ChatCommandService chatCommandService;
    private final ChatWebSocketService chatWebSocketService;
    private final ChatConverter chatConverter;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * StreamId로 userId 조회
     */
    public Long getUserIdByStreamId(String streamId) {
        StreamingSession session = activeSessions.get(streamId);
        return session != null ? session.getUserId() : null;
    }
    
    /**
     * 스트리밍 세션 시작
     */
    public String startStreaming(Long userId) {
        log.info("스트리밍 세션 시작: userId={}", userId);
        
        // 새 StreamId 생성
        String streamId = streamIdGenerator.generate();
        
        // 기존 세션은 자연스럽게 대체됨 - 단순히 새 세션 생성
        
        // 새 세션 생성
        StreamingSession session = new StreamingSession(userId);
        activeSessions.put(streamId, session);
        
        log.debug("스트리밍 세션 생성 완료: streamId={}, userId={}", streamId, userId);
        return streamId;
    }
    
    
    /**
     * 스트리밍 데이터 처리 (stream 이벤트)
     */
    public void processStreamData(AiStreamData streamData) {
        String streamId = streamData.streamId();

        StreamingSession session = activeSessions.get(streamId);
        if (session == null) {
            log.warn("활성 스트리밍 세션이 없음: streamId={}", streamId);
            throw new StreamException(StreamErrorCode.STREAM_SESSION_NOT_FOUND, null);
        }

        Long userId = session.getUserId();
        log.debug("스트리밍 데이터 처리: streamId={}, userId={}", streamId, userId);

        // 첫 번째 스트림 데이터인지 확인 (스트리밍 시작 알림)
        boolean isFirstData = session.getCurrentResponse().isEmpty();
        
        // 스트리밍 데이터 추가
        if (streamData.message() != null) {
            session.appendResponse(streamData.message());
        }
        
        // 첫 번째 데이터면 스트리밍 시작 알림 (이벤트 발행)
        if (isFirstData && streamData.message() != null) {
            eventPublisher.publishEvent(new StreamStartEvent(userId, streamId, streamData.message()));
        }
        
        // 클라이언트에 실시간 전송
        chatWebSocketService.sendStreamDataToUser(userId, streamData.message());
    }
    
    /**
     * 스트리밍 완료 처리 (done 이벤트)
     */
    public void processStreamComplete(AiStreamData streamData) {
        String streamId = streamData.streamId();
        log.debug("스트리밍 완료 처리: streamId={}", streamId);
        
        StreamingSession session = activeSessions.get(streamId);
        if (session == null) {
            log.warn("완료 처리 실패 - 세션 없음: streamId={}", streamId);
            return;
        }
        Long userId = session.getUserId();
        log.debug("스트리밍 완료 처리: streamId={}, userId={}", streamId, userId);

        
        // 스트리밍 완료 처리 - AI 응답 메시지 저장
        String finalResponse = session.getFinalResponse();
        if (finalResponse != null && !finalResponse.isBlank()) {
            try {
                Long botChatId = chatCommandService.saveBotMessage(session.getUserId(), finalResponse);
                StreamEndData endData = new StreamEndData(botChatId, LocalDateTime.now());
                chatWebSocketService.sendStreamEndDataToUser(userId , endData);
                log.info("AI 응답 메시지 저장 완됨: streamId={}, userId={}", streamId, session.getUserId());
                activeSessions.remove(streamId);
            } catch (Exception e) {
                log.error("AI 응답 메시지 저장 실패: streamId={}, userId={}, error={}", streamId, session.getUserId(), e.getMessage(), e);
                chatWebSocketService.sendStreamErrorToUser(userId, StreamErrorCode.STREAM_END_EVENT_FAIL);
                throw new ChatException(ChatErrorCode.MESSAGE_SAVE_FAIL, session.getUserId());
            }
        }
    }
    
    /**
     * 스트리밍 에러 처리 (error 이벤트)
     */
    public void processStreamError(AiStreamData streamData) {
        String streamId = streamData.streamId();
        log.debug("스트리밍 에러 처리: streamId={}", streamId);
        
        StreamingSession session = activeSessions.get(streamId);
        if (session == null) {
            log.warn("에러 처리 실패 - 세션 없음: streamId={}", streamId);
            return;
        }
        
        Long userId = session.getUserId();
        log.debug("스트리밍 에러 처리: streamId={}, userId={}", streamId, userId);
        
        String errorMessage = streamData.message() != null ?
            streamData.message() : "알 수 없는 오류가 발생했습니다";
        
        // 에러 메시지 전송
        log.debug("Ai Stream중 에러 발생: userId={}, error={}", userId, errorMessage);
        chatWebSocketService.sendStreamErrorToUser(userId, StreamErrorCode.AI_SERVER_ERROR);
        
        // 스트리밍 중단
        stopStreaming(streamId);
    }
    
    /**
     * 스트리밍 세션 중단 (streamId 기반) - 세션 직접 삭제
     */
    public void stopStreaming(String streamId) {
        log.info("스트리밍 세션 중단: streamId={}", streamId);
        
        // 세션 직접 삭제 - cleanup 불필요
        StreamingSession removedSession = activeSessions.remove(streamId);
        if (removedSession != null) {
            log.debug("세션 삭제 완료: streamId={}, userId={}", streamId, removedSession.getUserId());
        }
    }
    
    
    /**
     * 스트리밍 세션 취소 (streamId 기반) - 세션 직접 삭제
     */
    public void cancelStreaming(String streamId) {
        log.info("스트리밍 세션 취소: streamId={}", streamId);
        
        // 취소 요청이 온 세션 삭제
        StreamingSession removedSession = activeSessions.remove(streamId);
        if (removedSession != null) {
            log.debug("세션 취소 삭제 완료: streamId={}, userId={}", streamId, removedSession.getUserId());
        }
    }
    
    /**
     * 30초마다 타임아웃 세션 체크 및 정리
     * - 마지막 응답으로부터 31초 경과한 세션들을 자동 제거
     */
    @Scheduled(fixedRate = 30000) // 30초
    public void checkTimeoutSessions() {
        LocalDateTime now = LocalDateTime.now();
        int initialSize = activeSessions.size();
        log.debug("타임아웃 세션 체크 시작, 현재 세션 수: {}", initialSize);
        
        boolean hasRemovals = activeSessions.entrySet().removeIf(entry -> {
            String streamId = entry.getKey();
            StreamingSession session = entry.getValue();
            
            // 마지막 응답으로부터 31초 경과 시 타임아웃 (1초 여유)
            long secondsSinceLastResponse = Duration.between(session.getLastResponseTime(), now).toSeconds();
            
            if (secondsSinceLastResponse > 31) {
                log.warn("타임아웃 세션 제거: streamId={}, userId={}, 마지막응답={}초전", 
                    streamId, session.getUserId(), secondsSinceLastResponse);
                
                // 타임아웃 에러 전송
                try {
                    chatWebSocketService.sendStreamErrorToUser(session.getUserId(), StreamErrorCode.AI_SERVER_TIMEOUT);
                    log.info("타임아웃 에러 전송 완료: streamId={}, userId={}", streamId, session.getUserId());
                } catch (Exception e) {
                    log.error("타임아웃 에러 전송 실패: streamId={}, userId={}, error={}", 
                        streamId, session.getUserId(), e.getMessage());
                }
                
                return true; // 세션 제거
            }
            
            return false;
        });
        
        int finalSize = activeSessions.size();
        int timeoutCount = initialSize - finalSize;
        
        if (hasRemovals) {
            log.info("타임아웃 세션 정리 완료: {}개 세션 제거, 남은 세션 수: {}", timeoutCount, finalSize);
        } else {
            log.debug("타임아웃 세션 정리 완료: 제거할 세션 없음, 현재 세션 수: {}", finalSize);
        }
    }
    
    /**
     * 주기적으로 장기 실행 세션 정리 (10분마다)
     * - 10분 이상 지속된 세션들을 자동 제거
     */
    @Scheduled(fixedRate = 600000) // 10분
    public void cleanupLongRunningSessions() {
        int initialSize = activeSessions.size();
        log.debug("장기 실행 세션 정리 시작, 현재 세션 수: {}", initialSize);
        
        boolean hasRemovals = activeSessions.entrySet().removeIf(entry -> {
            StreamingSession session = entry.getValue();
            String streamId = entry.getKey();
            
            // 10분 이상 실행된 세션 제거
            if (session.getDurationSeconds() > 600) {
                log.warn("장기 실행 세션 제거: streamId={}, userId={}, duration={}초", 
                    streamId, session.getUserId(), session.getDurationSeconds());
                return true;
            }
            
            return false;
        });
        
        int finalSize = activeSessions.size();
        int cleanupCount = initialSize - finalSize;
        
        if (hasRemovals) {
            log.info("장기 실행 세션 정리 완료: {}개 세션 제거, 남은 세션 수: {}", cleanupCount, finalSize);
        } else {
            log.debug("장기 실행 세션 정리 완료: 제거할 세션 없음, 현재 세션 수: {}", finalSize);
        }
    }
}