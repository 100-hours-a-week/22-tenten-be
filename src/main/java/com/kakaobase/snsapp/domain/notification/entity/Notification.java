package com.kakaobase.snsapp.domain.notification.entity;

import com.kakaobase.snsapp.domain.notification.util.NotificationType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", columnDefinition = "INT UNSIGNED")
    private Long id;

    @Column(name = "receiver_id", nullable = false, columnDefinition = "INT UNSIGNED")
    private Long receiverId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private NotificationType notificationType;

    @Column(name = "target_type", nullable = false, length = 20)
    private String targetType;

    @Column(name = "target_id", nullable = false, columnDefinition = "INT UNSIGNED")
    private Long targetId;

    @Column(name = "is_read", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean isRead = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public Notification(Long receiverId, NotificationType type, Long targetId) {
        this.receiverId = receiverId;
        this.notificationType = type;
        this.targetType = type.getTargetType();
        this.targetId = targetId;
        this.isRead = false;
    }

    // 알림 읽음 처리
    public void markAsRead() {
        this.isRead = true;
    }

    // 알림 읽음 상태 확인
    public boolean isUnread() {
        return !this.isRead;
    }
}