package com.kakaobase.snsapp.domain.notification.dto.records;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * 알림 응답 데이터의 공통 인터페이스
 * NotificationData와 NotificationFollowingData를 타입 안전하게 통합
 */
public sealed interface NotificationResponse 
    permits ContentNotification, FollowingNotificationData {

    /**
     * 알림 고유 ID
     */
    Long id();
    
    /**
     * 읽음 여부
     */
    Boolean isRead();
    
    /**
     * 알림 생성 시간
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime timestamp();
}