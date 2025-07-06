package com.kakaobase.snsapp.domain.notification.controller;

import com.kakaobase.snsapp.domain.notification.converter.NotificationConverter;
import com.kakaobase.snsapp.domain.notification.dto.records.NotificationAckData;
import com.kakaobase.snsapp.domain.notification.dto.records.NotificationNackData;
import com.kakaobase.snsapp.domain.notification.dto.records.NotificationRequestData;
import com.kakaobase.snsapp.domain.notification.error.NotificationException;
import com.kakaobase.snsapp.domain.notification.service.NotificationService;
import com.kakaobase.snsapp.global.common.entity.WebSocketPacket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Slf4j
@Controller
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notifService;
    private final NotificationConverter notifConverter;

    @MessageMapping("/notification.read")
    @SendToUser("/queue/notification")
    public WebSocketPacket<NotificationAckData> notificationReadHandler(@Payload WebSocketPacket<NotificationRequestData> request, Principal principal) {
        return notifService.readNotification(request);
    }

    @MessageMapping("/notification.remove")
    @SendToUser("/queue/notification")
    public WebSocketPacket<NotificationAckData> notificationRemoveHandler(@Payload WebSocketPacket<NotificationRequestData> request, Principal principal) {
        return notifService.removeNotification(request);
    }

    @MessageExceptionHandler(NotificationException.class)
    @SendToUser("/queue/notification")
    public WebSocketPacket<NotificationNackData> handleBusinessException(NotificationException ex, Principal principal) {
        log.warn("비즈니스 에러 - 사용자: {}, 에러: {}", principal.getName(), ex.getMessage());

        return notifConverter.toNackData(ex.getErorrCode(), ex.getNotificationId());
    }
}
