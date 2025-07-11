package com.kakaobase.snsapp.domain.chat.event;

import com.kakaobase.snsapp.domain.chat.service.communication.ChatWebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 채팅 이벤트 리스너
 * 채팅 관련 이벤트를 수신하여 적절한 처리를 수행
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatEventListener {
    
    private final ChatWebSocketService chatWebSocketService;
    
    /**
     * LoadingEvent 처리
     * AI 서버로 ChatBlock 전송 시작 시 사용자에게 로딩 상태 알림
     */
    @Async
    @EventListener
    public void handleLoadingEvent(LoadingEvent event) {
        log.info("LoadingEvent 수신: userId={}, streamId={}, messageLength={}", 
            event.getUserId(), event.getStreamId(), 
            event.getMessageContent() != null ? event.getMessageContent().length() : 0);
        
        // 사용자에게 로딩 상태 알림 전송
        chatWebSocketService.sendLoadingToUser(event.getUserId());
    }
    
    /**
     * StreamStartEvent 처리
     * AI 서버로부터 첫 번째 스트림 응답 수신 시 사용자에게 스트리밍 시작 알림
     */
    @Async
    @EventListener
    public void handleStreamStartEvent(StreamStartEvent event) {
        log.info("StreamStartEvent 수신: userId={}, streamId={}, firstDataLength={}", 
            event.getUserId(), event.getStreamId(), 
            event.getFirstStreamData() != null ? event.getFirstStreamData().length() : 0);
        
        // 사용자에게 스트림 시작 알림 전송
        chatWebSocketService.sendStreamStartToUser(event.getUserId(), event.getStreamId());
    }
}