package com.kakaobase.snsapp.domain.notification.converter;

import com.kakaobase.snsapp.domain.members.dto.MemberResponseDto;
import com.kakaobase.snsapp.domain.notification.dto.records.NotificationData;
import com.kakaobase.snsapp.domain.notification.dto.records.NotificationFollowingData;
import com.kakaobase.snsapp.domain.notification.entity.Notification;
import com.kakaobase.snsapp.domain.notification.util.NotificationType;
import com.kakaobase.snsapp.global.common.entity.WebSocketPacket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationConverter {

    public Notification toEntity(Long receiverId, NotificationType type, Long targetId){
        return Notification.builder()
                .receiverId(receiverId)
                .type(type)
                .targetId(targetId)
                .build();
    }

    public WebSocketPacket<NotificationData> toPacket(Long notifId, NotificationType type, Long targetId, String content, MemberResponseDto.UserInfo userInfo){

        var data = NotificationData.builder()
                .id(notifId)
                .sender(userInfo)
                .content(content)
                .timestamp(LocalDateTime.now())
                .target_id(targetId)
                .build();

        return WebSocketPacket.<NotificationData>builder()
                .event(type.getEvent())
                .data(data)
                .build();
    }

    public WebSocketPacket<NotificationFollowingData> toPacket(Long notifId, NotificationType type, Long targetId, String content, MemberResponseDto.UserInfoWithFollowing userInfo){

        var data = NotificationFollowingData.builder()
                .id(notifId)
                .sender(userInfo)
                .content(content)
                .timestamp(LocalDateTime.now())
                .target_id(targetId)
                .build();

        return WebSocketPacket.<NotificationFollowingData>builder()
                .event(type.getEvent())
                .data(data)
                .build();
    }
}
