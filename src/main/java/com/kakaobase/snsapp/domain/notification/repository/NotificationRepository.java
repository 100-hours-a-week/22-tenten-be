package com.kakaobase.snsapp.domain.notification.repository;

import com.kakaobase.snsapp.domain.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {


}
