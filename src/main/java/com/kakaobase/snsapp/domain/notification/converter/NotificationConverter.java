package com.kakaobase.snsapp.domain.notification.converter;

import com.kakaobase.snsapp.domain.members.dto.MemberResponseDto;
import com.kakaobase.snsapp.domain.notification.dto.records.NotificationAckData;
import com.kakaobase.snsapp.domain.notification.dto.records.ContentNotification;
import com.kakaobase.snsapp.domain.notification.dto.records.FollowingNotificationData;
import com.kakaobase.snsapp.domain.notification.dto.records.NotificationNackData;
import com.kakaobase.snsapp.domain.notification.dto.records.NotificationResponse;
import com.kakaobase.snsapp.domain.notification.entity.Notification;
import com.kakaobase.snsapp.domain.notification.error.NotificationErrorCode;
import com.kakaobase.snsapp.domain.notification.util.NotificationType;
import com.kakaobase.snsapp.domain.notification.util.ResponseEnum;
import com.kakaobase.snsapp.global.common.entity.WebSocketPacket;
import com.kakaobase.snsapp.global.common.entity.WebSocketPacketImpl;
import com.kakaobase.snsapp.global.error.code.ErrorPacketData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationConverter {

    public Notification toEntity(Long receiverId, Long senderId, NotificationType type, Long targetId){
        return Notification.builder()
                .receiverId(receiverId)
                .senderId(senderId)
                .type(type)
                .targetId(targetId)
                .build();
    }

    public WebSocketPacket<ContentNotification> toNewPacket(Long notifId, NotificationType type, Long targetId, String content, MemberResponseDto.UserInfo userInfo){

        var data = ContentNotification.builder()
                .event(type.getEvent())
                .id(notifId)
                .sender(userInfo)
                .content(content)
                .timestamp(LocalDateTime.now())
                .target_id(targetId)
                .isRead(false)
                .build();

        return new WebSocketPacketImpl<>(type.getEvent(), data);
    }

    public WebSocketPacket<FollowingNotificationData> toNewPacket(Long notifId, NotificationType type, MemberResponseDto.UserInfoWithFollowing userInfo){

        var data = FollowingNotificationData.builder()
                .event(type.getEvent())
                .id(notifId)
                .sender(userInfo)
                .isRead(false)
                .timestamp(LocalDateTime.now())
                .build();

        return new WebSocketPacketImpl<>(type.getEvent(), data);
    }

    //기존 Notification을 Dto로 변환
    public WebSocketPacket<FollowingNotificationData> toPacket(Long notifId, NotificationType type, MemberResponseDto.UserInfoWithFollowing userInfo, LocalDateTime timestamp, Boolean isRead){

        var data = FollowingNotificationData.builder()
                .event(type.getEvent())
                .id(notifId)
                .sender(userInfo)
                .timestamp(timestamp)
                .isRead(isRead)
                .build();

        return new WebSocketPacketImpl<>(type.getEvent(), data);
    }


    public WebSocketPacket<ContentNotification> toPacket(Long notifId, NotificationType type, Long targetId, String content, MemberResponseDto.UserInfo userInfo, LocalDateTime timestamp, Boolean isRead){

        var data = ContentNotification.builder()
                .event(type.getEvent())
                .id(notifId)
                .sender(userInfo)
                .content(content)
                .timestamp(timestamp)
                .target_id(targetId)
                .isRead(isRead)
                .build();

        return new WebSocketPacketImpl<>(type.getEvent(), data);
    }

    // NotificationResponse 타입으로 반환하는 메서드 
    public WebSocketPacket<NotificationResponse> toNotificationResponsePacket(Long notifId, NotificationType type, Long targetId, String content, MemberResponseDto.UserInfo userInfo, LocalDateTime timestamp, Boolean isRead){
        var data = ContentNotification.builder()
                .event(type.getEvent())
                .id(notifId)
                .sender(userInfo)
                .content(content)
                .timestamp(timestamp)
                .target_id(targetId)
                .isRead(isRead)
                .build();

        return new WebSocketPacketImpl<>(type.getEvent(), (NotificationResponse) data);
    }

    // 팔로우 알림용 메서드 추가
    public WebSocketPacket<NotificationResponse> toFollowingResponsePacket(Long notifId, NotificationType type, MemberResponseDto.UserInfoWithFollowing userInfo, LocalDateTime timestamp, Boolean isRead){
        var data = FollowingNotificationData.builder()
                .event(type.getEvent())
                .id(notifId)
                .sender(userInfo)
                .timestamp(timestamp)
                .isRead(isRead)
                .build();

        return new WebSocketPacketImpl<>(type.getEvent(), (NotificationResponse) data);
    }

    public WebSocketPacketImpl<NotificationAckData> toAckPacket(Long notifId, ResponseEnum responseEnum){
        var data = NotificationAckData.builder()
                .id(notifId)
                .message(responseEnum.getMessage())
                .timestamp(LocalDateTime.now())
                .build();

        return new WebSocketPacketImpl<>(responseEnum.getEvent(), data);
    }

    public WebSocketPacket<NotificationNackData> toNackPacket(NotificationErrorCode errorCode, Long notifId){
        var data = NotificationNackData.builder()
                .id(notifId)
                .error(errorCode.getError())
                .message(errorCode.getMessage())
                .timestamp(LocalDateTime.now())
                .build();

        return new WebSocketPacketImpl<>(errorCode.getEvent(), data);
    }

    public WebSocketPacketImpl<ErrorPacketData> toErrorPacket(NotificationErrorCode errorCode){
        var data = ErrorPacketData.builder()
                .error(errorCode.getError())
                .message(errorCode.getMessage())
                .timestamp(LocalDateTime.now())
                .build();

        return new WebSocketPacketImpl<>(errorCode.getEvent(), data);
    }
}
