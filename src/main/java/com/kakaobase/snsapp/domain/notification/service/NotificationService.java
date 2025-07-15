package com.kakaobase.snsapp.domain.notification.service;

import com.kakaobase.snsapp.domain.members.dto.MemberResponseDto;
import com.kakaobase.snsapp.domain.notification.converter.NotificationConverter;
import com.kakaobase.snsapp.domain.notification.dto.records.*;
import com.kakaobase.snsapp.domain.notification.dto.response.NotificationFetchResponse;
import com.kakaobase.snsapp.domain.notification.error.NotificationErrorCode;
import com.kakaobase.snsapp.domain.notification.error.NotificationException;
import com.kakaobase.snsapp.domain.notification.util.InvalidNotificationCacheUtil;
import com.kakaobase.snsapp.domain.notification.util.NotificationType;
import com.kakaobase.snsapp.domain.notification.util.ResponseEnum;
import com.kakaobase.snsapp.global.common.entity.WebSocketPacket;
import com.kakaobase.snsapp.global.common.entity.WebSocketPacketImpl;
import com.kakaobase.snsapp.global.error.code.GeneralErrorCode;
import com.kakaobase.snsapp.global.error.exception.CustomException;
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
    private final InvalidNotificationCacheUtil invalidNotificationCacheUtil;

    public NotificationFetchResponse getNotifList(Long memberId, int limit, Long cursor) {
        if (limit < 1) {
            throw new CustomException(GeneralErrorCode.INVALID_QUERY_PARAMETER, "limit", "limit는 1 이상이어야 합니다.");
        }

        try {
            // 1. limit+10개 알림 조회 (hasNext 판단을 위해)
            List<NotificationResponse> allNotifications = commandService.findNotificationsByUserId(memberId, limit + 10);
            
            // 2. cursor 기반 필터링 (cursor보다 작은 ID만)
            List<NotificationResponse> filteredNotifications = allNotifications.stream()
                .filter(notification -> cursor == null || notification.id() < cursor)
                .toList();
            
            // 3. hasNext 판단 (limit+1개가 있는지 확인)
            boolean hasNext = filteredNotifications.size() > limit;
            
            // 4. limit만큼 자르기
            List<NotificationResponse> finalNotifications = filteredNotifications.stream()
                .limit(limit)
                .toList();
            
            // 5. unreadCount 계산 (최종 반환될 알림 중에서)
            int unreadCount = (int) finalNotifications.stream()
                .filter(notification -> !notification.isRead())
                .count();
            
            // 6. NotificationFetchResponse 생성
            NotificationFetchResponse response = NotificationFetchResponse.builder()
                .unreadCount(unreadCount)
                .hasNext(hasNext)
                .notifications(finalNotifications)
                .build();
                
            log.info("사용자 {}의 알림 {}개 조회됨 (반환: {}개, 읽지 않은: {}개, hasNext: {})",
                    memberId, allNotifications.size(), finalNotifications.size(), unreadCount, hasNext);
                    
            return response;

        } catch (Exception e) {
            log.error("사용자 {}의 알림 조회 중 오류 발생", memberId, e);
            throw new NotificationException(NotificationErrorCode.NOTIFICATION_FETCH_FAIL);
        }
    }


    public void sendCommentCreatedNotification(Long receiverId, Long targetId, String content, MemberResponseDto.UserInfo userInfo, Long postId) {
        try{
            sendNotification(receiverId, targetId, content, userInfo, NotificationType.COMMENT_CREATED, postId);
        } catch (Exception e) {
            commandService.sendNotificationError(NotificationErrorCode.NOTIFICATION_SEND_FAIL, receiverId);
            throw new NotificationException(NotificationErrorCode.NOTIFICATION_SEND_FAIL);
        }
    }

    public void sendRecommentCreatedNotification(Long receiverId, Long targetId, String content, MemberResponseDto.UserInfo userInfo, Long postId) {
        try{
            sendNotification(receiverId, targetId, content, userInfo, NotificationType.RECOMMENT_CREATED, postId);
        } catch (Exception e) {
            commandService.sendNotificationError(NotificationErrorCode.NOTIFICATION_SEND_FAIL, receiverId);
            throw new NotificationException(NotificationErrorCode.NOTIFICATION_SEND_FAIL);
        }
    }

    public void sendPostLikeCreatedNotification(Long receiverId, Long targetId, String content, MemberResponseDto.UserInfo userInfo, Long postId) {
        try{
            sendNotification(receiverId, targetId, content, userInfo, NotificationType.POST_LIKE_CREATED, postId);
        } catch (Exception e) {
            commandService.sendNotificationError(NotificationErrorCode.NOTIFICATION_SEND_FAIL, receiverId);
            throw new NotificationException(NotificationErrorCode.NOTIFICATION_SEND_FAIL);
        }
    }

    public void sendCommentLikeCreatedNotification(Long receiverId, Long targetId, String content, MemberResponseDto.UserInfo userInfo, Long postId) {
        try{
            sendNotification(receiverId, targetId, content, userInfo, NotificationType.COMMENT_LIKE_CREATED, postId);
        } catch (Exception e) {
            commandService.sendNotificationError(NotificationErrorCode.NOTIFICATION_SEND_FAIL, receiverId);
            throw new NotificationException(NotificationErrorCode.NOTIFICATION_SEND_FAIL);
        }
    }

    public void sendRecommentLikeCreatedNotification(Long receiverId, Long targetId, String content, MemberResponseDto.UserInfo userInfo, Long postId) {
        try{
            sendNotification(receiverId, targetId, content, userInfo, NotificationType.RECOMMENT_LIKE_CREATED, postId);
        } catch (Exception e) {
            commandService.sendNotificationError(NotificationErrorCode.NOTIFICATION_SEND_FAIL, receiverId);
            throw new NotificationException(NotificationErrorCode.NOTIFICATION_SEND_FAIL);
        }
    }

    public void sendFollowingCreatedNotification(Long receiverId, Long targetId, MemberResponseDto.UserInfoWithFollowing userInfo) {
        try{
            sendFollowNotification(receiverId, targetId, userInfo, NotificationType.FOLLOWING_CREATED);
        } catch (Exception e) {
            commandService.sendNotificationError(NotificationErrorCode.NOTIFICATION_SEND_FAIL, receiverId);
            throw new NotificationException(NotificationErrorCode.NOTIFICATION_SEND_FAIL);
        }
    }

    private void sendNotification(Long receiverId, Long targetId, String content, MemberResponseDto.UserInfo userInfo, NotificationType type, Long postId) {
        Long notifId = commandService.createNotification(receiverId, userInfo.id(), type, targetId);
        commandService.sendNotification(receiverId, notifId, type, content, postId, userInfo);
    }

    //팔로우 알림용
    private void sendFollowNotification(Long receiverId, Long targetId, MemberResponseDto.UserInfoWithFollowing userInfo, NotificationType type) {
        Long notifId = commandService.createNotification(receiverId, userInfo.id(), type, targetId);
        commandService.sendNotification(receiverId, notifId, type, userInfo);
    }


    public WebSocketPacket<NotificationAckData> readNotification(WebSocketPacket<NotificationRequestData> packet) {
        try{
            commandService.updateNotificationRead(packet.data.id());
            return notifConverter.toAckPacket(packet.data.id(), ResponseEnum.READ_SUCCESS);
        } catch (Exception e){
            throw new NotificationException(NotificationErrorCode.NOTIFICATION_READ_FAIL, packet.data.id() );
        }
    }

    public WebSocketPacket<NotificationAckData> removeNotification(WebSocketPacket<NotificationRequestData> packet) {
        try{
            commandService.deleteNotification(packet.data.id());
            return notifConverter.toAckPacket(packet.data.id(), ResponseEnum.REMOVE_SUCCESS);
        } catch (Exception e){
            log.error("에러 삭제 처리중 예외 발생 알림id: {}, 에러: {}", packet.data.id(), e.getMessage());
            throw new NotificationException(NotificationErrorCode.NOTIFICATION_DELETE_FAIL, packet.data.id());
        }
    }
}