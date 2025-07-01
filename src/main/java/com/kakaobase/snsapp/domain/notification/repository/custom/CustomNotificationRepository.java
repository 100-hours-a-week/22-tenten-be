package com.kakaobase.snsapp.domain.notification.repository.custom;

import com.kakaobase.snsapp.global.common.entity.WebSocketPacket;

import java.util.List;

/**
 * 알림 복잡한 쿼리를 위한 Custom Repository
 */
public interface CustomNotificationRepository {

    /**
     * 특정 사용자의 모든 알림을 WebSocketPacket List 형태로 조회
     *
     * @param userId 사용자 ID
     * @return WebSocketPacket으로 wrapping된 알림 리스트
     */
    List<WebSocketPacket<?>> findAllNotificationsByUserId(Long userId);
}