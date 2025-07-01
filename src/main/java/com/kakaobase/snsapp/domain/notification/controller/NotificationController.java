package com.kakaobase.snsapp.domain.notification.controller;

import com.kakaobase.snsapp.domain.notification.dto.records.NotificationRequestData;
import com.kakaobase.snsapp.domain.notification.dto.records.NotificationResponseData;
import com.kakaobase.snsapp.domain.notification.service.NotificationService;
import com.kakaobase.snsapp.global.common.entity.WebSocketPacket;
import com.kakaobase.snsapp.global.common.entity.WebSocketPacketImpl;
import com.kakaobase.snsapp.global.error.code.ErrorPacketData;
import com.kakaobase.snsapp.global.error.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.LocalDateTime;

@Slf4j
@Controller
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notifService;

    @MessageMapping("/notification.read")
    @SendToUser("/queue/notification")
    public WebSocketPacket<NotificationResponseData> notificationReadHandler(@Payload WebSocketPacket<NotificationRequestData> request, Principal principal) {
        return notifService.readNotification(request);
    }

    @MessageExceptionHandler(Exception.class)
    @SendToUser("/queue/errors")
    public WebSocketPacket<ErrorPacketData> handleBusinessException(CustomException ex, Principal principal) {
        log.warn("비즈니스 에러 - 사용자: {}, 에러: {}", principal.getName(), ex.getMessage());

        var errorData = ErrorPacketData.builder()
                .error("internal_server_error")
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();

        return new WebSocketPacketImpl<>("internal_server_error", errorData);
    }
}
