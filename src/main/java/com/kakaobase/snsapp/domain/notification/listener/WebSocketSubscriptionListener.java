package com.kakaobase.snsapp.domain.notification.listener;

import com.kakaobase.snsapp.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketSubscriptionListener {

    private final NotificationService notificationService;

    /**
     * 클라이언트가 구독할 때 발생하는 이벤트 처리
     */
    @EventListener
    public void handleSubscriptionEvent(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        
        String destination = headerAccessor.getDestination();
        String sessionId = headerAccessor.getSessionId();
        String userId = headerAccessor.getUser() != null ? headerAccessor.getUser().getName() : "anonymous";
        
        log.info("구독 이벤트 - 사용자: {}, 세션: {}, 구독 경로: {}", userId, sessionId, destination);
        
        // 알림 큐 구독시 해당 사용자의 모든 알림 전송
        if (destination != null && destination.equals("/user/queue/notification")) {
            log.info("알림 큐 구독 감지 - HTTP API로 대체됨: 사용자 {}", userId);
            // TODO: WebSocket 기반 자동 알림 전송은 HTTP API로 대체되었습니다.
            // 클라이언트는 GET /api/users/notifications 엔드포인트를 사용해야 합니다.
        }
    }
}