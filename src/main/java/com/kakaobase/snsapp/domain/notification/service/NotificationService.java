package com.kakaobase.snsapp.domain.notification.service;

import com.kakaobase.snsapp.domain.members.dto.MemberResponseDto;
import com.kakaobase.snsapp.domain.notification.dto.records.NotificationRequestData;
import com.kakaobase.snsapp.domain.notification.repository.NotificationRepository;
import com.kakaobase.snsapp.domain.notification.util.NotificationType;
import com.kakaobase.snsapp.global.common.entity.WebSocketPacket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notifRepository;
    private final NotificationCommandService commandService;

    public void sendCommentCreatedNotification(Long receiverId, Long postId, String content, MemberResponseDto.UserInfo userInfo) {
        NotificationType type = NotificationType.COMMENT_CREATED;
        Long notifId = commandService.createNotification(receiverId, type, postId);
        commandService.sendNotification(notifId, type, content, postId, userInfo);
    }

    public void sendRecommentCreatedNotification(Long receiverId, Long postId, String content, MemberResponseDto.UserInfo userInfo) {
        NotificationType type = NotificationType.RECOMMENT_CREATED;
        Long notifId = commandService.createNotification(receiverId, type, postId);
        commandService.sendNotification(notifId, type, content, postId, userInfo);
    }

    public void sendPostLikeCreatedNotification(Long receiverId, Long postId, String content, MemberResponseDto.UserInfo userInfo) {
        NotificationType type = NotificationType.POST_LIKE_CREATED;
        Long notifId = commandService.createNotification(receiverId, type, postId);
        commandService.sendNotification(notifId, type, content, postId, userInfo);
    }


    public void sendCommentLikeCreatedNotification(Long receiverId, Long postId, String content, MemberResponseDto.UserInfo userInfo) {
        NotificationType type = NotificationType.COMMENT_LIKE_CREATED;
        Long notifId = commandService.createNotification(receiverId, type, postId);
        commandService.sendNotification(notifId, type, content, postId, userInfo);
    }

    public void sendRecommentLikeCreatedNotification(Long receiverId, Long postId, String content, MemberResponseDto.UserInfo userInfo) {
        NotificationType type = NotificationType.RECOMMENT_LIKE_CREATED;
        Long notifId = commandService.createNotification(receiverId, type, postId);
        commandService.sendNotification(notifId, type, content, postId, userInfo);
    }

    public void sendFollowingCreatedNotification(Long receiverId, Long postId, String content, MemberResponseDto.UserInfoWithFollowing userInfo) {

        NotificationType type = NotificationType.FOLLOWING_CREATED;
        Long notifId = commandService.createNotification(receiverId, type, postId);
        commandService.sendNotification(notifId, type, content, postId, userInfo);
    }


    public void markNotificationRead(WebSocketPacket<NotificationRequestData> packet) {

        commandService.updateNotificationRead(packet.data.id());
    }
}
