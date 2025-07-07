package com.kakaobase.snsapp.domain.notification.scheduler;

import com.kakaobase.snsapp.domain.notification.service.NotificationCleanupService;
import com.kakaobase.snsapp.domain.notification.util.InvalidNotificationCacheUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Set;

/**
 * 알림 정리 스케줄러
 * Redis 캐시 기반 무효 알림 정리 (1시간마다) + 주기적 DB 정리 (매일 오후 9시, 매주 일요일)
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "notification.cleanup.enabled", havingValue = "true", matchIfMissing = true)
public class NotificationCleanupScheduler {

    private final NotificationCleanupService notificationCleanupService;
    private final InvalidNotificationCacheUtil invalidNotificationCacheUtil;

    /**
     * 1시간마다 Redis 캐시에서 무효 알림 ID를 조회하고 실제 DB에서 삭제
     * fixedRate = 3600000ms = 1시간
     */
    @Scheduled(fixedRate = 3600000)
    public void cleanupInvalidNotificationsFromCache() {
        log.info("Redis 캐시 기반 무효 알림 정리 작업 시작 - 1시간마다");
        
        try {
            // 현재 Redis 캐시 상태 로깅
            Long cacheSize = invalidNotificationCacheUtil.getInvalidNotificationCount();
            log.info("현재 Redis 캐시에 저장된 무효 알림 ID 개수: {}", cacheSize);
            
            // Redis에서 무효 알림 ID 목록 조회 및 클리어
            Set<Long> invalidIds = invalidNotificationCacheUtil.getAllInvalidNotificationIdsAndClear();
            
            if (!invalidIds.isEmpty()) {
                // 실제 DB에서 삭제
                int deletedCount = notificationCleanupService.deleteNotificationsByIds(
                    new ArrayList<>(invalidIds)
                );
                log.info("Redis 캐시 기반 무효 알림 {}개 정리 완료 (실제 삭제: {}개)", 
                        invalidIds.size(), deletedCount);
            } else {
                log.info("Redis 캐시에 정리할 무효 알림이 없습니다");
            }
        } catch (Exception e) {
            log.error("Redis 캐시 기반 알림 정리 중 오류 발생", e);
        }
    }


    /**
     * 매주 일요일 새벽 3시에 일주일 이상 오래된 알림 정리
     * cron: 초 분 시 일 월 요일 (0 = 일요일)
     */
    @Scheduled(cron = "0 0 3 * * 0")
    public void cleanupOldNotifications() {
        log.info("스케줄된 오래된 알림 정리 작업 시작 - 매주 일요일 새벽 3시");
        
        try {
            int deletedCount = notificationCleanupService.cleanupOldNotifications(7);
            log.info("스케줄된 오래된 알림 정리 작업 성공적으로 완료 - {}개 삭제", deletedCount);
        } catch (Exception e) {
            log.error("스케줄된 오래된 알림 정리 작업 중 오류 발생", e);
        }
    }
}