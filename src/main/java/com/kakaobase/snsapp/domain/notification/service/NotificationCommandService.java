package com.kakaobase.snsapp.domain.notification.service;


import com.kakaobase.snsapp.domain.members.dto.MemberResponseDto;
import com.kakaobase.snsapp.domain.notification.converter.NotificationConverter;
import com.kakaobase.snsapp.domain.notification.dto.records.NotificationData;
import com.kakaobase.snsapp.domain.notification.dto.records.NotificationFollowingData;
import com.kakaobase.snsapp.domain.notification.entity.Notification;
import com.kakaobase.snsapp.domain.notification.error.NotificationException;
import com.kakaobase.snsapp.domain.notification.repository.NotificationRepository;
import com.kakaobase.snsapp.domain.notification.util.NotificationType;
import com.kakaobase.snsapp.global.common.entity.WebSocketPacket;
import com.kakaobase.snsapp.global.common.entity.WebSocketPacketImpl;
import com.kakaobase.snsapp.global.error.code.GeneralErrorCode;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationCommandService {

    private final NotificationConverter notificationConverter;
    private final NotificationRepository notifRepository;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final NotificationRepository notificationRepository;

    @Transactional
    public Long createNotification(Long receiverId, NotificationType type, Long targetId) {
        Notification notification = notificationConverter.toEntity(receiverId, type, targetId);
        notifRepository.save(notification);
        return notification.getId();
    }

    @Transactional
    public void updateNotificationRead(Long notifId) {
        Notification notification = notificationRepository.findById(notifId)
                .orElseThrow(()-> new NotificationException(GeneralErrorCode.INTERNAL_SERVER_ERROR));

        notification.markAsRead();
    }

    @Async
    public void sendNotification(Long receiverId, Long notifId, NotificationType type, String content, Long targetId, MemberResponseDto.UserInfo userInfo) {
        WebSocketPacket<NotificationData> packet = notificationConverter.toNewPacket(notifId, type, targetId, content, userInfo);
        simpMessagingTemplate.convertAndSendToUser(receiverId.toString(), "/queue/notifications", packet);
    }

    @Async
    public void sendNotification(Long receiverId, Long notifId, NotificationType type, MemberResponseDto.UserInfoWithFollowing userInfo) {
        WebSocketPacket<NotificationFollowingData> packet = notificationConverter.toNewPacket(notifId, type, userInfo);
        simpMessagingTemplate.convertAndSendToUser(receiverId.toString(), "/queue/notifications", packet);
    }

    /**
     * 사용자의 모든 알림을 WebSocket으로 전송
     */
    @Async
    public void sendAllNotificationsToUser(Long userId) {
        log.info("사용자 {}의 모든 알림 조회 및 전송", userId);
        
        try {
            // 사용자의 모든 알림을 WebSocketPacket List로 조회
            List<WebSocketPacket<?>> notifications = 
                notificationRepository.findAllNotificationsByUserId(userId);
            
            log.info("사용자 {}의 알림 {}개 조회됨", userId, notifications.size());
            
            // 최종 응답 형식: WebSocketPacketImpl로 감싸서 전송
            WebSocketPacketImpl<List<WebSocketPacket<?>>> finalPacket = 
                new WebSocketPacketImpl<>("notification.fetch", notifications);
            
            // WebSocket으로 전송
            simpMessagingTemplate.convertAndSendToUser(userId.toString(), "/queue/notifications", finalPacket);
            
            log.info("사용자 {}에게 모든 알림 전송 완료", userId);
            
        } catch (Exception e) {
            log.error("사용자 {}의 모든 알림 전송 실패", userId, e);
            throw e;
        }
    }
}
