package com.kakaobase.snsapp.domain.notification.repository.custom;

import com.kakaobase.snsapp.domain.notification.dto.records.NotificationResponse;
import com.kakaobase.snsapp.global.common.entity.WebSocketPacket;

import java.util.List;

/**
 * 알림 복잡한 쿼리를 위한 Custom Repository
 */
public interface CustomNotificationRepository {

    /**
     * 특정 사용자의 모든 알림을 NotificationResponse List 형태로 조회 (기존 호환성)
     *
     * @param userId 사용자 ID
     * @return NotificationResponse 리스트
     */
    List<NotificationResponse> findAllNotificationsByUserId(Long userId);

    /**
     * 특정 사용자의 알림을 cursor 기반 페이지네이션으로 조회
     *
     * @param userId 사용자 ID
     * @param limit 조회할 알림 개수 (hasNext 판단을 위해 +1개 더 조회)
     * @param cursor 마지막으로 조회한 알림 ID (null이면 최신부터 조회)
     * @return NotificationResponse 리스트
     */
    List<NotificationResponse> findNotificationsByUserIdWithPagination(Long userId, Integer limit, Long cursor);
}