package com.kakaobase.snsapp.domain.notification.controller;

import com.kakaobase.snsapp.domain.auth.principal.CustomUserDetails;
import com.kakaobase.snsapp.domain.notification.converter.NotificationConverter;
import com.kakaobase.snsapp.domain.notification.dto.records.NotificationAckData;
import com.kakaobase.snsapp.domain.notification.dto.records.ContentNotification;
import com.kakaobase.snsapp.domain.notification.dto.records.NotificationNackData;
import com.kakaobase.snsapp.domain.notification.dto.records.NotificationRequestData;
import com.kakaobase.snsapp.domain.notification.dto.response.NotificationFetchResponse;
import com.kakaobase.snsapp.domain.notification.error.NotificationException;
import com.kakaobase.snsapp.domain.notification.service.NotificationService;
import com.kakaobase.snsapp.global.common.entity.WebSocketPacket;
import com.kakaobase.snsapp.global.common.response.CustomResponse;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notifService;
    private final NotificationConverter notifConverter;

    @GetMapping("/api/users/notifications")
    public CustomResponse<NotificationFetchResponse> getNotifications(
            @Parameter(description = "한 페이지에 표시할 알림 수") @RequestParam(defaultValue = "20") int limit,
            @Parameter(description = "마지막으로 조회한 알림 ID") @RequestParam(required = false) Long cursor,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ){
        Long memberId = Long.valueOf(userDetails.getId());
        NotificationFetchResponse response = notifService.getNotifList(memberId, limit, cursor);

        return CustomResponse.success("알림을 불러오는데 성공하였습니다", response);
    }

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

        return notifConverter.toNackPacket(ex.getErorrCode(), ex.getNotificationId());
    }
}
