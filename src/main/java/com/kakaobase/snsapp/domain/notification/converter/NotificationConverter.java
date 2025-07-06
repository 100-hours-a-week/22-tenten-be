package com.kakaobase.snsapp.domain.notification.converter;

import com.kakaobase.snsapp.domain.members.dto.MemberResponseDto;
import com.kakaobase.snsapp.domain.notification.dto.records.NotificationAckData;
import com.kakaobase.snsapp.domain.notification.dto.records.NotificationData;
import com.kakaobase.snsapp.domain.notification.dto.records.NotificationFollowingData;
import com.kakaobase.snsapp.domain.notification.dto.records.NotificationNackData;
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

    public WebSocketPacket<NotificationData> toNewPacket(Long notifId, NotificationType type, Long targetId, String content, MemberResponseDto.UserInfo userInfo){

        var data = NotificationData.builder()
                .id(notifId)
                .sender(userInfo)
                .content(content)
                .timestamp(LocalDateTime.now())
                .target_id(targetId)
                .isRead(false)
                .build();

        return new WebSocketPacketImpl<>(type.getEvent(), data);
    }

    public WebSocketPacket<NotificationFollowingData> toNewPacket(Long notifId, NotificationType type, MemberResponseDto.UserInfoWithFollowing userInfo){

        var data = NotificationFollowingData.builder()
                .id(notifId)
                .sender(userInfo)
                .isRead(false)
                .timestamp(LocalDateTime.now())
                .build();

        return new WebSocketPacketImpl<>(type.getEvent(), data);
    }

    //기존 Notification을 Dto로 변환
    public WebSocketPacket<NotificationFollowingData> toPacket(Long notifId, NotificationType type, MemberResponseDto.UserInfoWithFollowing userInfo, LocalDateTime timestamp, Boolean isRead){

        var data = NotificationFollowingData.builder()
                .id(notifId)
                .sender(userInfo)
                .timestamp(timestamp)
                .isRead(isRead)
                .build();

        return new WebSocketPacketImpl<>(type.getEvent(), data);
    }


    public WebSocketPacket<NotificationData> toPacket(Long notifId, NotificationType type, Long targetId, String content, MemberResponseDto.UserInfo userInfo, LocalDateTime timestamp, Boolean isRead){

        var data = NotificationData.builder()
                .id(notifId)
                .sender(userInfo)
                .content(content)
                .timestamp(timestamp)
                .target_id(targetId)
                .isRead(isRead)
                .build();

        return new WebSocketPacketImpl<>(type.getEvent(), data);
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
