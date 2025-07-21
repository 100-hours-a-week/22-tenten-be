package com.kakaobase.snsapp.domain.notification.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kakaobase.snsapp.domain.notification.dto.records.NotificationResponse;
import com.kakaobase.snsapp.global.common.entity.WebSocketPacket;
import lombok.Builder;

import java.util.List;

/**
 * HTTP 알림 조회 API 응답 DTO
 * cursor 기반 페이지네이션을 지원하는 알림 목록 응답
 */
@Builder
public record NotificationFetchResponse(
        @JsonProperty("unread_count")
        Integer unreadCount,
        @JsonProperty("has_next")
        Boolean hasNext,
        List<WebSocketPacket<NotificationResponse>> notifications
) {}