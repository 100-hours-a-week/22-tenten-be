package com.kakaobase.snsapp.domain.notification.service;

import com.kakaobase.snsapp.domain.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 알림 정리 서비스
 * Redis 캐시에 저장된 무효 알림 ID를 기반으로 일괄 삭제 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationCleanupService {

    private final NotificationRepository notificationRepository;

    /**
     * 특정 알림 ID 목록을 기반으로 알림 삭제
     * Redis 캐시에서 가져온 무효 알림 ID 목록을 일괄 삭제
     * 
     * @param notificationIds 삭제할 알림 ID 목록
     * @return 삭제된 알림 개수
     */
    @Transactional
    public int deleteNotificationsByIds(List<Long> notificationIds) {
        if (notificationIds == null || notificationIds.isEmpty()) {
            log.debug("삭제할 알림 ID 목록이 비어있습니다");
            return 0;
        }
        
        log.info("무효 알림 ID {}개를 기반으로 일괄 삭제 시작", notificationIds.size());
        
        try {
            // 삭제 전 존재하는 알림 개수 확인
            long existingCount = notificationRepository.countByIdIn(notificationIds);
            
            // Repository의 deleteAllById 사용하여 일괄 삭제
            notificationRepository.deleteAllById(notificationIds);
            
            log.info("무효 알림 일괄 삭제 완료 - 요청: {}개, 존재했던 알림: {}개", 
                    notificationIds.size(), existingCount);
            return (int) existingCount;
            
        } catch (Exception e) {
            log.error("무효 알림 일괄 삭제 중 오류 발생 - 요청 개수: {}", notificationIds.size(), e);
            throw e;
        }
    }

    /**
     * 오래된 알림 정리 (선택적 기능)
     * 지정된 일수보다 오래된 알림을 삭제
     * 
     * @param daysOld 며칠 이전의 알림을 삭제할지
     * @return 삭제된 알림 개수
     */
    @Transactional
    public int cleanupOldNotifications(int daysOld) {
        log.info("{}일 이전의 오래된 알림 정리 시작", daysOld);
        
        try {
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysOld);
            
            // Repository의 날짜 기반 삭제 메서드 사용
            int deletedCount = notificationRepository.deleteByCreatedAtBefore(cutoffDate);
            
            log.info("오래된 알림 정리 완료 - {}일 이전 알림 {}개 삭제", daysOld, deletedCount);
            return deletedCount;
            
        } catch (Exception e) {
            log.error("오래된 알림 정리 중 오류 발생 - 기준: {}일", daysOld, e);
            throw e;
        }
    }
}