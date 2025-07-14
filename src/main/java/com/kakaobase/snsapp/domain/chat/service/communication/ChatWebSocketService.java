package com.kakaobase.snsapp.domain.chat.service.communication;

import com.kakaobase.snsapp.domain.chat.dto.SimpTimeData;
import com.kakaobase.snsapp.domain.chat.dto.ai.response.AiStreamData;
import com.kakaobase.snsapp.domain.chat.dto.response.ChatErrorData;
import com.kakaobase.snsapp.domain.chat.dto.response.StreamData;
import com.kakaobase.snsapp.domain.chat.dto.response.StreamEndData;
import com.kakaobase.snsapp.domain.chat.dto.response.StreamStartData;
import com.kakaobase.snsapp.domain.chat.exception.errorcode.ChatErrorCode;
import com.kakaobase.snsapp.domain.chat.exception.errorcode.StreamErrorCode;
import com.kakaobase.snsapp.domain.chat.util.ChatEventType;
import com.kakaobase.snsapp.global.common.entity.WebSocketPacketImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 채팅 WebSocket 전송 전용 서비스
 * 사용자와의 실시간 통신 담당
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatWebSocketService {
    
    private final SimpMessagingTemplate messagingTemplate;
    
    private static final String CHAT_QUEUE_DESTINATION = "/queue/chat";
    
    /**
     * 사용자에게 로딩 상태 전송
     */
    @Async
    public void sendLoadingToUser(Long userId) {
        log.info("사용자 로딩 알림 전송 시작: userId={}, 전송 시간={}, 스레드={}", 
            userId, System.currentTimeMillis(), Thread.currentThread().getName());
        
        try {
            SimpTimeData data = new SimpTimeData(LocalDateTime.now());
            WebSocketPacketImpl<SimpTimeData> packet = new WebSocketPacketImpl<>(
                ChatEventType.CHAT_STREAM_LOADING.getEvent(), data);
            
            log.info("WebSocket 패킷 생성 완료: userId={}, event={}, destination={}", 
                userId, ChatEventType.CHAT_STREAM_LOADING.getEvent(), CHAT_QUEUE_DESTINATION);
            
            messagingTemplate.convertAndSendToUser(userId.toString(), CHAT_QUEUE_DESTINATION, packet);
            log.info("사용자 로딩 알림 전송 완료: userId={}, 완료 시간={}", userId, System.currentTimeMillis());
        } catch (Exception e) {
            log.error("사용자 로딩 알림 전송 실패: userId={}, error={}", userId, e.getMessage(), e);
        }
    }
    
    /**
     * 사용자에게 스트림 시작 패킷 전송
     */
    @Async
    public void sendStreamStartToUser(Long userId, String streamId) {
        log.info("사용자 스트림 시작 알림 전송: userId={}, streamId={}, 전송 시간={}", 
            userId, streamId, System.currentTimeMillis());
        
        try {
            StreamStartData data = new StreamStartData(streamId, LocalDateTime.now());
            WebSocketPacketImpl<StreamStartData> packet = new WebSocketPacketImpl<>(
                ChatEventType.CHAT_STREAM_START.getEvent(), data);
            
            log.info("스트림 시작 패킷 생성 완료: userId={}, streamId={}, event={}", 
                userId, streamId, ChatEventType.CHAT_STREAM_START.getEvent());
            
            messagingTemplate.convertAndSendToUser(userId.toString(), CHAT_QUEUE_DESTINATION, packet);
            log.info("사용자 스트림 시작 알림 전송 완료: userId={}, streamId={}", userId, streamId);
        } catch (Exception e) {
            log.error("사용자 스트림 시작 알림 전송 실패: userId={}, streamId={}, error={}", 
                userId, streamId, e.getMessage(), e);
        }
    }
    
    /**
     * 사용자에게 스트림 데이터 전송
     */
    @Async
    public void sendStreamDataToUser(Long userId, String content) {
        log.info("사용자 스트림 데이터 전송: userId={}, contentLength={}, 전송 시간={}", 
            userId, content != null ? content.length() : 0, System.currentTimeMillis());
        
        try {
            StreamData streamData = new StreamData(content, LocalDateTime.now());
            WebSocketPacketImpl<StreamData> packet = new WebSocketPacketImpl<>(
                ChatEventType.CHAT_STREAM.getEvent(), streamData);
            
            log.info("스트림 데이터 패킷 생성 완료: userId={}, event={}", 
                userId, ChatEventType.CHAT_STREAM.getEvent());
            
            messagingTemplate.convertAndSendToUser(userId.toString(), CHAT_QUEUE_DESTINATION, packet);
            log.info("사용자 스트림 데이터 전송 완료: userId={}, contentLength={}", 
                userId, content != null ? content.length() : 0);
        } catch (Exception e) {
            log.error("사용자 스트림 데이터 전송 실패: userId={}, contentLength={}, error={}", 
                userId, content != null ? content.length() : 0, e.getMessage(), e);
        }
    }
    
    /**
     * 사용자에게 커스텀 이벤트로 스트림 패킷 전송
     */
    @Async
    public void sendStreamEndDataToUser(Long userId, StreamEndData streamEndData) {
        log.debug("Stream종료 데이터 전송: userId={}", userId);
        
        WebSocketPacketImpl<StreamEndData> packet = new WebSocketPacketImpl<>(ChatEventType.CHAT_STREAM_END.getEvent(), streamEndData);
        messagingTemplate.convertAndSendToUser(userId.toString(), CHAT_QUEUE_DESTINATION, packet);
    }
    
    /**
     * 사용자에게 스트림 종료 패킷 전송
     */
    @Async
    public void sendStreamEndToUser(Long userId) {
        log.debug("사용자 스트림 종료 알림 전송: userId={}", userId);
        
        SimpTimeData data = new SimpTimeData(LocalDateTime.now());
        WebSocketPacketImpl<SimpTimeData> packet = new WebSocketPacketImpl<>(
            ChatEventType.CHAT_STREAM_END.getEvent(), data);
        
        messagingTemplate.convertAndSendToUser(userId.toString(), CHAT_QUEUE_DESTINATION, packet);
    }
    
    /**
     * 사용자에게 채팅 에러 메시지 전송
     */
    @Async
    public void sendChatErrorToUser(Long userId, ChatErrorCode errorCode) {
        log.debug("사용자 채팅 에러 알림 전송: userId={}, errorCode={}", userId, errorCode);
        
        ChatErrorData data = new ChatErrorData(errorCode.getError(), errorCode.getMessage(), LocalDateTime.now());
        WebSocketPacketImpl<ChatErrorData> packet = new WebSocketPacketImpl<>(
            ChatEventType.CHAT_STREAM_ERROR.getEvent(), data);
        
        messagingTemplate.convertAndSendToUser(userId.toString(), CHAT_QUEUE_DESTINATION, packet);
    }
    
    /**
     * 사용자에게 스트림 에러 메시지 전송
     */
    @Async
    public void sendStreamErrorToUser(Long userId, StreamErrorCode errorCode) {
        log.debug("사용자 스트림 에러 알림 전송: userId={}, errorCode={}", userId, errorCode);
        
        ChatErrorData data = new ChatErrorData(errorCode.getError(), errorCode.getMessage(), LocalDateTime.now());
        WebSocketPacketImpl<ChatErrorData> packet = new WebSocketPacketImpl<>(
            ChatEventType.CHAT_STREAM_ERROR.getEvent(), data);
        
        messagingTemplate.convertAndSendToUser(userId.toString(), CHAT_QUEUE_DESTINATION, packet);
    }
}
