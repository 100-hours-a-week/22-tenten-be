package com.kakaobase.snsapp.domain.notification.service;


import com.kakaobase.snsapp.domain.members.dto.MemberResponseDto;
import com.kakaobase.snsapp.domain.notification.converter.NotificationConverter;
import com.kakaobase.snsapp.domain.notification.dto.records.NotificationData;
import com.kakaobase.snsapp.domain.notification.dto.records.NotificationFetchData;
import com.kakaobase.snsapp.domain.notification.dto.records.NotificationFollowingData;
import com.kakaobase.snsapp.domain.notification.entity.Notification;
import com.kakaobase.snsapp.domain.notification.error.NotificationErrorCode;
import com.kakaobase.snsapp.domain.notification.error.NotificationException;
import com.kakaobase.snsapp.domain.notification.repository.NotificationRepository;
import com.kakaobase.snsapp.domain.notification.util.NotificationType;
import com.kakaobase.snsapp.global.common.entity.WebSocketPacket;
import com.kakaobase.snsapp.global.common.entity.WebSocketPacketImpl;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.View;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationCommandService {

    private final NotificationConverter notifConverter;
    private final NotificationRepository notifRepository;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final NotificationRepository notificationRepository;

    private static final String NOTIFY_SUBSCRIBE_PATH = "/queue/notification";
    private final View error;

    @Transactional
    public Long createNotification(Long receiverId, NotificationType type, Long targetId) {
        Notification notification = notifConverter.toEntity(receiverId, type, targetId);
        notifRepository.save(notification);
        return notification.getId();
    }

    @Transactional
    public void updateNotificationRead(Long notifId) {
        Notification notification = notificationRepository.findById(notifId)
                .orElseThrow(()-> new NotificationException(NotificationErrorCode.NOTIFICATION_NOT_FOUND));

        notification.markAsRead();
    }

    @Transactional
    public void deleteNotification(Long notifId) {
        Notification notification = notificationRepository.findById(notifId)
                .orElseThrow(()-> new NotificationException(NotificationErrorCode.NOTIFICATION_NOT_FOUND));

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
        WebSocketPacket<NotificationData> packet = notifConverter.toNewPacket(notifId, type, targetId, content, userInfo);
        simpMessagingTemplate.convertAndSendToUser(receiverId.toString(), NOTIFY_SUBSCRIBE_PATH, packet);
    }

    @Async
    public void sendNotification(Long receiverId, Long notifId, NotificationType type, MemberResponseDto.UserInfoWithFollowing userInfo) {
        WebSocketPacket<NotificationFollowingData> packet = notifConverter.toNewPacket(notifId, type, userInfo);
        simpMessagingTemplate.convertAndSendToUser(receiverId.toString(), NOTIFY_SUBSCRIBE_PATH, packet);
    }


    @Transactional
    public List<WebSocketPacket<?>> getAllNotificationsToUser(Long userId) {
        log.info("사용자 {}의 모든 알림 조회 및 전송", userId);
        
        try {
            // 사용자의 모든 알림을 WebSocketPacket List로 조회
            return notificationRepository.findAllNotificationsByUserId(userId);

        } catch (Exception e) {
            log.error("사용자 {}의 모든 알림 전송 실패", userId, e);
            throw new NotificationException(NotificationErrorCode.NOTIFICATION_FETCH_FAIL);
        }
    }

    /**
     * 무효한 알림들을 비동기로 일괄 삭제
     */
    @Async
    @Transactional
    public void deleteInvalidNotifications(List<Long> invalidNotificationIds) {
        try {
            log.info("무효한 알림 {}개 비동기 삭제 시작", invalidNotificationIds.size());
            notifRepository.deleteAllById(invalidNotificationIds);
            log.info("무효한 알림 {}개 비동기 삭제 완료", invalidNotificationIds.size());
        } catch (Exception e) {
            log.error("무효한 알림 삭제 중 오류 발생: {}", invalidNotificationIds, e);
            throw new NotificationException(NotificationErrorCode.NOTIFICATION_DELETE_FAIL);
        }
    }

    /**
     * NotificationFetchData를 WebSocket으로 전송
     */
    @Async
    public void sendNotificationFetchData(Long userId, WebSocketPacketImpl<NotificationFetchData> packet) {
        try {
            simpMessagingTemplate.convertAndSendToUser(userId.toString(), NOTIFY_SUBSCRIBE_PATH, packet);
            log.info("사용자 {}에게 알림 데이터 전송 완료 (총 {}개, 읽지 않은 {}개)", 
                    userId, packet.data.notifications().size(), packet.data.unread_count());
        } catch (Exception e) {
            log.error("사용자 {}에게 알림 데이터 전송 실패", userId, e);
            throw new NotificationException(NotificationErrorCode.NOTIFICATION_FETCH_FAIL);
        }
    }
}
