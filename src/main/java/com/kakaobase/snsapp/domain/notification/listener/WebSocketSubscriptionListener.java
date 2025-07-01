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
        if (destination != null && destination.equals("/user/queue/notifications")) {
            log.info("알림 큐 구독 감지 - 모든 알림 전송 시작: 사용자 {}", userId);
            
            try {
                // 사용자의 모든 알림 전송 (읽음/안읽음 상관없이)
                notificationService.sendAllNotifications(Long.valueOf(userId));
                log.info("모든 알림 전송 완료: 사용자 {}", userId);
            } catch (Exception e) {
                log.error("모든 알림 전송 실패: 사용자 {}, 에러: {}", userId, e.getMessage(), e);
            }
        }
    }
}