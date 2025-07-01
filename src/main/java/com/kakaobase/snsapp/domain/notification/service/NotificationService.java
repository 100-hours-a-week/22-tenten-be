package com.kakaobase.snsapp.domain.notification.service;

import com.kakaobase.snsapp.domain.members.dto.MemberResponseDto;
import com.kakaobase.snsapp.domain.notification.converter.NotificationConverter;
import com.kakaobase.snsapp.domain.notification.dto.records.NotificationRequestData;
import com.kakaobase.snsapp.domain.notification.dto.records.NotificationResponseData;
import com.kakaobase.snsapp.domain.notification.error.NotificationException;
import com.kakaobase.snsapp.domain.notification.util.NotificationType;
import com.kakaobase.snsapp.domain.notification.util.ResponseEnum;
import com.kakaobase.snsapp.global.common.entity.WebSocketPacket;
import com.kakaobase.snsapp.global.error.code.GeneralErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationCommandService commandService;
    private final NotificationConverter notifConverter;

    public void sendCommentCreatedNotification(Long receiverId, Long postId, String content, MemberResponseDto.UserInfo userInfo) {
        sendNotification(receiverId, postId, content, userInfo, NotificationType.COMMENT_CREATED);
    }

    public void sendRecommentCreatedNotification(Long receiverId, Long postId, String content, MemberResponseDto.UserInfo userInfo) {
        sendNotification(receiverId, postId, content, userInfo, NotificationType.RECOMMENT_CREATED);
    }

    public void sendPostLikeCreatedNotification(Long receiverId, Long postId, String content, MemberResponseDto.UserInfo userInfo) {
        sendNotification(receiverId, postId, content, userInfo, NotificationType.POST_LIKE_CREATED);
    }

    public void sendCommentLikeCreatedNotification(Long receiverId, Long postId, String content, MemberResponseDto.UserInfo userInfo) {
        sendNotification(receiverId, postId, content, userInfo, NotificationType.COMMENT_LIKE_CREATED);
    }

    public void sendRecommentLikeCreatedNotification(Long receiverId, Long postId, String content, MemberResponseDto.UserInfo userInfo) {
        sendNotification(receiverId, postId, content, userInfo, NotificationType.RECOMMENT_LIKE_CREATED);
    }

    public void sendFollowingCreatedNotification(Long receiverId, Long postId, String content, MemberResponseDto.UserInfoWithFollowing userInfo) {
        sendNotification(receiverId, postId, content, userInfo, NotificationType.FOLLOWING_CREATED);
    }

    private void sendNotification(Long receiverId, Long postId, String content, MemberResponseDto.UserInfo userInfo, NotificationType type) {
        Long notifId = commandService.createNotification(receiverId, type, postId);
        commandService.sendNotification(receiverId, notifId, type, content, postId, userInfo);
    }

    private void sendNotification(Long receiverId, Long postId, String content, MemberResponseDto.UserInfoWithFollowing userInfo, NotificationType type) {
        Long notifId = commandService.createNotification(receiverId, type, postId);
        commandService.sendNotification(receiverId, notifId, type, content, postId, userInfo);
    }


    public WebSocketPacket<NotificationResponseData> readNotification(WebSocketPacket<NotificationRequestData> packet) {
        try{
            commandService.updateNotificationRead(packet.data.id());
            return notifConverter.toResponsePacket(packet.data.id(), ResponseEnum.READ_SUCCESS.getEvent(), null, ResponseEnum.READ_SUCCESS.getMessage());
        } catch (NotificationException e){
            log.error("에러 읽음 처리중 예외 발생 알림id: {}, 에러 코드: {}", packet.data.id(), e.getErrorCode().toString() );
            return notifConverter.toResponsePacket(packet.data.id(), ResponseEnum.READ_FAIL.getEvent(), e.getErrorCode().toString(), ResponseEnum.READ_FAIL.getMessage());
        } catch (Exception e){
            return notifConverter.toResponsePacket(packet.data.id(), ResponseEnum.READ_FAIL.getEvent(), GeneralErrorCode.INTERNAL_SERVER_ERROR.getError(), ResponseEnum.READ_FAIL.getMessage());
        }

    }

    /**
     * 사용자의 모든 알림을 WebSocket으로 전송
     */
    public void sendAllNotifications(Long userId) {
        log.info("사용자 {}의 모든 알림 전송 시작", userId);
        
        try {
            commandService.sendAllNotificationsToUser(userId);
            log.info("사용자 {}의 모든 알림 전송 완료", userId);
        } catch (Exception e) {
            log.error("사용자 {}의 모든 알림 전송 중 오류 발생", userId, e);
        }
    }
}
