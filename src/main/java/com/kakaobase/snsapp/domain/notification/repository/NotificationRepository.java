package com.kakaobase.snsapp.domain.notification.repository;

import com.kakaobase.snsapp.domain.notification.entity.Notification;
import com.kakaobase.snsapp.domain.notification.repository.custom.CustomNotificationRepository;
import com.kakaobase.snsapp.domain.notification.util.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long>, CustomNotificationRepository {

    /**
     * 알림 타입과 타겟 ID로 알림 목록 조회
     */
    List<Notification> findByNotificationTypeAndTargetId(NotificationType notificationType, Long targetId);

    /**
     * 알림 타입, 타겟 ID, 수신자 ID로 특정 알림 조회
     */
    Optional<Notification> findByNotificationTypeAndTargetIdAndReceiverId(NotificationType notificationType, Long targetId, Long receiverId);

}
