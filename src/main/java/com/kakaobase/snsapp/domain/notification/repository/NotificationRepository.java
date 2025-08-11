package com.kakaobase.snsapp.domain.notification.repository;

import com.kakaobase.snsapp.domain.notification.entity.Notification;
import com.kakaobase.snsapp.domain.notification.repository.custom.CustomNotificationRepository;
import com.kakaobase.snsapp.domain.notification.util.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long>, CustomNotificationRepository {

    /**
     * 알림 타입, 타겟 ID, 수신자 ID로 특정 알림 조회
     */
    Optional<Notification> findByNotificationTypeAndTargetIdAndReceiverId(NotificationType notificationType, Long targetId, Long receiverId);

    /**
     * 특정 ID 목록에 해당하는 알림 개수 조회
     */
    long countByIdIn(List<Long> ids);

    /**
     * 특정 날짜 이전의 알림 삭제
     */
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.createdAt < :cutoffDate")
    int deleteByCreatedAtBefore(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * 특정 사용자의 안읽은 알림 개수 조회
     */
    Long countByReceiverIdAndIsRead(Long receiverId, Boolean isRead);

}
