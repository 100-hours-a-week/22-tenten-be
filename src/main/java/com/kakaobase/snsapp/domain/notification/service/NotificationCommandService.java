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
import com.kakaobase.snsapp.global.error.code.GeneralErrorCode;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

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
        WebSocketPacket<NotificationData> packet = notificationConverter.toPacket(notifId, type, targetId, content, userInfo);
        simpMessagingTemplate.convertAndSendToUser(receiverId.toString(), "/queue/notifications", packet);
    }

    @Async
    public void sendNotification(Long receiverId, Long notifId, NotificationType type, String content, Long targetId, MemberResponseDto.UserInfoWithFollowing userInfo) {
        WebSocketPacket<NotificationFollowingData> packet = notificationConverter.toPacket(notifId, type, targetId, content, userInfo);
        simpMessagingTemplate.convertAndSendToUser(receiverId.toString(), "/queue/notifications", packet);
    }
}
