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
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationCommandService {

    private final NotificationConverter notificationConverter;
    private final NotificationRepository notifRepository;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final NotificationRepository notificationRepository;

    private static final String NOTIFY_SUBSCRIBE_PATH = "/queue/notification";

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

    @Transactional
    public void deleteNotification(Long notifId) {
        Notification notification = notificationRepository.findById(notifId)
                .orElseThrow(()-> new NotificationException(GeneralErrorCode.INTERNAL_SERVER_ERROR));

        notifRepository.delete(notification);
    }

    /**
     * 특정 타입, 타겟 ID, 수신자 ID로 알림 삭제 (오버로딩)
     */
    @Transactional
    public void deleteNotification(NotificationType notificationType, Long targetId, Long receiverId) {
        Optional<Notification> notification = notificationRepository.findByNotificationTypeAndTargetIdAndReceiverId(notificationType, targetId, receiverId);
        
        if (notification.isPresent()) {
            notifRepository.delete(notification.get());
            log.info("알림 삭제 완료 - 타입: {}, 타겟ID: {}, 수신자ID: {}", 
                    notificationType, targetId, receiverId);
        } else {
            log.debug("삭제할 알림이 없음 - 타입: {}, 타겟ID: {}, 수신자ID: {}", 
                    notificationType, targetId, receiverId);
        }
    }

    @Async
    public void sendNotification(Long receiverId, Long notifId, NotificationType type, String content, Long targetId, MemberResponseDto.UserInfo userInfo) {
        WebSocketPacket<NotificationData> packet = notificationConverter.toNewPacket(notifId, type, targetId, content, userInfo);
        simpMessagingTemplate.convertAndSendToUser(receiverId.toString(), NOTIFY_SUBSCRIBE_PATH, packet);
    }

    @Async
    public void sendNotification(Long receiverId, Long notifId, NotificationType type, MemberResponseDto.UserInfoWithFollowing userInfo) {
        WebSocketPacket<NotificationFollowingData> packet = notificationConverter.toNewPacket(notifId, type, userInfo);
        simpMessagingTemplate.convertAndSendToUser(receiverId.toString(), NOTIFY_SUBSCRIBE_PATH, packet);
    }

    /**
     * 사용자의 모든 알림을 WebSocket으로 전송
     */
    @Async
    public void sendAllNotificationsToUser(Long userId) {
        log.info("사용자 {}의 모든 알림 조회 및 전송", userId);
        
        try {
            // 사용자의 모든 알림을 WebSocketPacket List로 조회
            List<WebSocketPacket<?>> allNotifications = 
                notificationRepository.findAllNotificationsByUserId(userId);
            
            // 유효성 검사 및 필터링
            List<WebSocketPacket<?>> validNotifications = filterValidNotifications(allNotifications);
            
            log.info("사용자 {}의 알림 {}개 조회됨 (유효한 알림: {}개)", userId, allNotifications.size(), validNotifications.size());
            
            // 최종 응답 형식: WebSocketPacketImpl로 감싸서 전송
            WebSocketPacketImpl<List<WebSocketPacket<?>>> finalPacket = 
                new WebSocketPacketImpl<>("notification.fetch", validNotifications);
            
            // WebSocket으로 전송
            simpMessagingTemplate.convertAndSendToUser(userId.toString(), NOTIFY_SUBSCRIBE_PATH, finalPacket);
            
            log.info("사용자 {}에게 모든 알림 전송 완료", userId);
            
        } catch (Exception e) {
            log.error("사용자 {}의 모든 알림 전송 실패", userId, e);
            throw e;
        }
    }

    /**
     * 유효한 알림만 필터링 (무효한 알림은 반환 목록에서만 제거)
     */
    private List<WebSocketPacket<?>> filterValidNotifications(List<WebSocketPacket<?>> notifications) {
        return notifications.stream()
            .filter(packet -> {
                // sender가 null인 알림 필터링
                if (packet.data instanceof NotificationData notificationData) {
                    if (notificationData.sender() == null) {
                        log.warn("무효한 알림 감지 - ID: {}, sender null (반환 목록에서 제외)", notificationData.id());
                        return false;
                    }
                } else if (packet.data instanceof NotificationFollowingData followingData) {
                    if (followingData.sender() == null) {
                        log.warn("무효한 팔로우 알림 감지 - ID: {}, sender null (반환 목록에서 제외)", followingData.id());
                        return false;
                    }
                }
                return true;
            })
            .toList();
    }

}
