package com.kakaobase.snsapp.domain.notification.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 무효한 알림 ID를 Redis Set에 캐싱하는 유틸리티
 * 1시간마다 배치로 정리하기 위해 임시 저장소 역할
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InvalidNotificationCacheUtil {

    private static final String CACHE_KEY = "notification:invalid:cleanup";
    private static final Duration TTL = Duration.ofHours(2);
    
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 무효한 알림 ID를 Redis Set에 추가
     * @param notificationId 무효한 알림 ID
     */
    public void addInvalidNotificationId(Long notificationId) {
        try {
            redisTemplate.opsForSet().add(CACHE_KEY, notificationId.toString());
            redisTemplate.expire(CACHE_KEY, TTL);
            
            log.debug("무효 알림 ID Redis 캐시에 추가: {}", notificationId);
        } catch (Exception e) {
            log.error("무효 알림 ID Redis 캐시 추가 실패: notificationId={}", notificationId, e);
        }
    }

    /**
     * 모든 무효한 알림 ID를 조회하고 캐시를 클리어
     * 스케줄러에서 배치 정리할 때 사용
     * @return 무효한 알림 ID Set
     */
    public Set<Long> getAllInvalidNotificationIdsAndClear() {
        try {
            // Redis Set에서 모든 멤버 조회
            Set<Object> rawIds = redisTemplate.opsForSet().members(CACHE_KEY);
            
            if (rawIds == null || rawIds.isEmpty()) {
                log.debug("Redis 캐시에 무효 알림 ID가 없습니다");
                return Set.of();
            }

            // String을 Long으로 변환
            Set<Long> notificationIds = rawIds.stream()
                    .map(Object::toString)
                    .map(Long::valueOf)
                    .collect(Collectors.toSet());

            // 캐시 클리어
            redisTemplate.delete(CACHE_KEY);
            
            log.info("Redis 캐시에서 무효 알림 ID {}개 조회 및 클리어 완료", notificationIds.size());
            return notificationIds;
            
        } catch (Exception e) {
            log.error("Redis 캐시에서 무효 알림 ID 조회 및 클리어 실패", e);
            return Set.of();
        }
    }

    /**
     * 현재 캐시된 무효 알림 ID 개수 조회
     * 모니터링 목적
     * @return 캐시된 무효 알림 ID 개수
     */
    public Long getInvalidNotificationCount() {
        try {
            Long count = redisTemplate.opsForSet().size(CACHE_KEY);
            log.debug("현재 Redis 캐시에 저장된 무효 알림 ID 개수: {}", count);
            return count != null ? count : 0L;
        } catch (Exception e) {
            log.error("Redis 캐시 무효 알림 ID 개수 조회 실패", e);
            return 0L;
        }
    }
}