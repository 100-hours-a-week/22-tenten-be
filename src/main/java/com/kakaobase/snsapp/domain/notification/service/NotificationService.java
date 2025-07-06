package com.kakaobase.snsapp.domain.notification.service;

import com.kakaobase.snsapp.domain.members.dto.MemberResponseDto;
import com.kakaobase.snsapp.domain.notification.converter.NotificationConverter;
import com.kakaobase.snsapp.domain.notification.dto.records.*;
import com.kakaobase.snsapp.domain.notification.error.NotificationErrorCode;
import com.kakaobase.snsapp.domain.notification.error.NotificationException;
import com.kakaobase.snsapp.domain.notification.util.NotificationType;
import com.kakaobase.snsapp.domain.notification.util.ResponseEnum;
import com.kakaobase.snsapp.global.common.entity.WebSocketPacket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationCommandService commandService;
    private final NotificationConverter notifConverter;

    public void sendCommentCreatedNotification(Long receiverId, Long targetId, String content, MemberResponseDto.UserInfo userInfo, Long postId) {
        sendNotification(receiverId, targetId, content, userInfo, NotificationType.COMMENT_CREATED, postId);
    }

    public void sendRecommentCreatedNotification(Long receiverId, Long targetId, String content, MemberResponseDto.UserInfo userInfo, Long postId) {
        sendNotification(receiverId, targetId, content, userInfo, NotificationType.RECOMMENT_CREATED, postId);
    }

    public void sendPostLikeCreatedNotification(Long receiverId, Long targetId, String content, MemberResponseDto.UserInfo userInfo, Long postId) {
        sendNotification(receiverId, targetId, content, userInfo, NotificationType.POST_LIKE_CREATED, postId);
    }

    public void sendCommentLikeCreatedNotification(Long receiverId, Long targetId, String content, MemberResponseDto.UserInfo userInfo, Long postId) {
        sendNotification(receiverId, targetId, content, userInfo, NotificationType.COMMENT_LIKE_CREATED, postId);
    }

    public void sendRecommentLikeCreatedNotification(Long receiverId, Long targetId, String content, MemberResponseDto.UserInfo userInfo, Long postId) {
        sendNotification(receiverId, targetId, content, userInfo, NotificationType.RECOMMENT_LIKE_CREATED, postId);
    }

    public void sendFollowingCreatedNotification(Long receiverId, Long targetId, MemberResponseDto.UserInfoWithFollowing userInfo) {
        sendFollowNotification(receiverId, targetId, userInfo, NotificationType.FOLLOWING_CREATED);
    }

    private void sendNotification(Long receiverId, Long targetId, String content, MemberResponseDto.UserInfo userInfo, NotificationType type, Long postId) {
        Long notifId = commandService.createNotification(receiverId, type, targetId);
        commandService.sendNotification(receiverId, notifId, type, content, postId, userInfo);
    }

    //팔로우 알림용
    private void sendFollowNotification(Long receiverId, Long targetId, MemberResponseDto.UserInfoWithFollowing userInfo, NotificationType type) {
        Long notifId = commandService.createNotification(receiverId, type, targetId);
        commandService.sendNotification(receiverId, notifId, type, userInfo);
    }


    public WebSocketPacket<NotificationAckData> readNotification(WebSocketPacket<NotificationRequestData> packet) {
        try{
            commandService.updateNotificationRead(packet.data.id());
            return notifConverter.toAckPacket(packet.data.id(), ResponseEnum.READ_SUCCESS);
        } catch (Exception e){
            throw new NotificationException(NotificationErrorCode.NOTIFICATION_READ_FAIL);
        }
    }

    public WebSocketPacket<NotificationAckData> removeNotification(WebSocketPacket<NotificationRequestData> packet) {
        try{
            commandService.deleteNotification(packet.data.id());
            return notifConverter.toAckPacket(packet.data.id(), ResponseEnum.REMOVE_SUCCESS);
        } catch (Exception e){
            log.error("에러 삭제 처리중 예외 발생 알림id: {}, 에러: {}", packet.data.id(), e.getMessage());
            throw new NotificationException(NotificationErrorCode.NOTIFICATION_DELETE_FAIL);
        }

    }

    /**
     * 사용자의 모든 알림을 WebSocket으로 전송
     */
    public void sendAllNotifications(Long userId) {
        log.info("사용자 {}의 모든 알림 전송 시작", userId);
        
        try {
            // 1. 알림 목록 조회
            List<WebSocketPacket<?>> allNotifications = commandService.getAllNotifications(userId);

            // 2. 필터링해서 유효한 알림과 무효한 알림 ID 분리
            List<WebSocketPacket<?>> filterResult = filterNotifications(allNotifications);

            log.info("사용자 {}의 알림 {}개 조회됨 (유효한 알림: {}개, 무효한 알림: {}개)",
                    userId, allNotifications.size(),
                    filterResult.validNotifications().size(),
                    filterResult.invalidNotificationIds().size());
            
            // 3. 무효한 알림들 비동기 삭제 처리
            if (!filterResult.invalidNotificationIds().isEmpty()) {
                deleteInvalidNotificationsAsync(filterResult.invalidNotificationIds());
            }
            
            // 4. 유효한 알림들 WebSocket 전송
            commandService.sendValidNotificationsToUser(userId, filterResult.validNotifications());
            
            log.info("사용자 {}의 모든 알림 전송 완료", userId);
        } catch (Exception e) {
            log.error("사용자 {}의 모든 알림 전송 중 오류 발생", userId, e);
        }
    }

    /**
     * 알림 목록을 필터링하여 유효한 알림과 무효한 알림 ID 분리
     */
    private List<WebSocketPacket<?>> filterNotifications(List<WebSocketPacket<?>> notifications) {
        List<WebSocketPacket<?>> validNotifications = new ArrayList<>();
        List<Long> invalidNotificationIds = new ArrayList<>();
        
        for (WebSocketPacket<?> packet : notifications) {
            boolean isValid = true;
            Long notificationId = null;
            
            // sender가 null인 알림 필터링
            if (packet.data instanceof NotificationData notificationData) {
                notificationId = notificationData.id();
                if (notificationData.sender() == null) {
                    log.warn("무효한 알림 감지 - ID: {}, sender null", notificationId);
                    isValid = false;
                }
            } else if (packet.data instanceof NotificationFollowingData followingData) {
                notificationId = followingData.id();
                if (followingData.sender() == null) {
                    log.warn("무효한 팔로우 알림 감지 - ID: {}, sender null", notificationId);
                    isValid = false;
                }
            }
            
            if (isValid) {
                validNotifications.add(packet);
            } else if (notificationId != null) {
                invalidNotificationIds.add(notificationId);
            }
        }
        
        return validNotifications;
    }
}
