package com.kakaobase.snsapp.global.common.redis.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
public abstract class ContentItemCacheService<T, K> {

    protected final RedisTemplate<String, Object> redisTemplate;
    protected final StringRedisTemplate stringRedisTemplate;
    protected final RedissonClient redissonClient;

    private static final Duration CACHE_TTL = Duration.ofHours(24);
    private static final String CACHE_LOCK_PREFIX = "lock:create-cache:";

    // ========== Abstract Methods (구현체에서 정의) ==========

    /**
     * 캐시 키 접두사 반환 (예: "post:stats:", "comment:stats:")
     */
    protected abstract String getCacheKeyPrefix();

    /**
     * Lock 키 접두사 반환 (예: "post", "comment")
     */
    protected abstract String getLockKeyPrefix();

    /**
     * 동기화 큐 키 반환 (예: "posts:need_sync", "comments:need_sync")
     */
    protected abstract String getSyncQueueKey();

    /**
     * ID로 엔티티 조회 후 캐시 데이터 생성
     */
    protected abstract void saveCacheToRedis(K id);

    /**
     * 캐시 데이터를 Redis에 저장
     */
    protected abstract void saveCacheToRedis(K id, T cacheData);

    /**
     * Redis에서 캐시 데이터 로드
     */
    protected abstract T loadCacheFromRedis(K id);

    // ========== Public API Methods ==========

    /**
     * 특정 필드 증가
     */
    public void incrementField(K id, String fieldName) {
        ensureCacheExistsAndExecute(id, () -> incrementFieldAndSync(id, fieldName));
    }

    /**
     * 특정 필드 감소
     */
    public void decrementField(K id, String fieldName) {
        ensureCacheExistsAndExecute(id, () -> decrementFieldAndSync(id, fieldName));
    }

    /**
     * 캐시 단건 조회
     */
    public T getStatsCache(K id) {
        ensureCacheExistsAndCreate(id);
        return loadCacheFromRedis(id);
    }

    /**
     * 캐시 삭제
     */
    public void deleteCache(K id) {
        try {
            String key = generateCacheKey(id);
            redisTemplate.delete(key);
            removeFromSyncQueue(id);
            log.debug("캐시 삭제 완료: id={}", id);
        } catch (Exception e) {
            log.error("캐시 삭제 실패: id={}", id, e);
        }
    }

    /**
     * 동기화 필요한 항목 목록 조회
     */
    public List<K> getItemsNeedingSync() {
        try {
            Set<String> itemIds = stringRedisTemplate.opsForSet().members(getSyncQueueKey());
            if (itemIds == null || itemIds.isEmpty()) {
                return Collections.emptyList();
            }

            return itemIds.stream()
                    .map(this::parseId)
                    .filter(Objects::nonNull)
                    .toList();

        } catch (Exception e) {
            log.error("동기화 필요 항목 목록 조회 실패: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    // ========== Cache Existence Management ==========

    /**
     * 캐시 존재 보장 후 작업 실행
     */
    protected void ensureCacheExistsAndExecute(K id, Runnable operation) {
        ensureCacheExistsAndCreate(id);
        operation.run();
    }

    /**
     * DB 기준으로 캐시 존재 보장
     */
    protected void ensureCacheExistsAndCreate(K id) {
        if (!existsCache(id)) {
            createCacheWithLock(id, () -> saveCacheToRedis(id));
        }
    }

    /**
     * 주어진 캐시 데이터로 캐시 존재 보장
     */
    protected void ensureCacheExistsAndCreate(K id, T cacheData) {
        if (!existsCache(id)) {
            createCacheWithLock(id, () -> saveCacheToRedis(id, cacheData));
        }
    }

    /**
     * 캐시 존재 여부 확인
     */
    protected boolean existsCache(K id) {
        try {
            String key = generateCacheKey(id);
            return redisTemplate.hasKey(key);
        } catch (Exception e) {
            log.warn("캐시 존재 여부 확인 실패: id={}", id, e);
            return false;
        }
    }

    // ========== Cache Creation with Lock ==========

    /**
     * Lock으로 보호된 캐시 생성
     */
    protected void createCacheWithLock(K id, Runnable cacheCreator) {
        String lockKey = CACHE_LOCK_PREFIX + getLockKeyPrefix() + ":" + id;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            if (lock.tryLock(3, 10, TimeUnit.SECONDS)) {
                try {
                    // Double-check pattern
                    if (!existsCache(id)) {
                        cacheCreator.run();
                    }
                } finally {
                    lock.unlock();
                }
            } else {
                throw new RuntimeException("캐시 생성 중입니다. 잠시 후 다시 시도해주세요.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("캐시 생성이 중단되었습니다.", e);
        }
    }

    // ========== Cache Operations ==========

    /**
     * 필드 값 증가 및 동기화 큐 추가
     */
    protected void incrementFieldAndSync(K id, String fieldName) {
        try {
            String key = generateCacheKey(id);
            redisTemplate.opsForHash().increment(key, fieldName, 1);
            redisTemplate.expire(key, CACHE_TTL);
            addToSyncQueue(id);

            log.debug("{} 증가 완료: id={}, increment={}", fieldName, id, (long) 1);

        } catch (Exception e) {
            log.error("{} 증가 실패: id={}", fieldName, id, e);
            throw new RuntimeException(fieldName + " 증가 실패", e);
        }
    }

    /**
     * 필드 값 감소 및 동기화 큐 추가 (음수 방지)
     */
    protected void decrementFieldAndSync(K id, String fieldName) {
        try {
            String key = generateCacheKey(id);
            Long newValue = redisTemplate.opsForHash().increment(key, fieldName, -(long) -1);

            // 음수 방지
            if (newValue < 0) {
                redisTemplate.opsForHash().put(key, fieldName, "0");
                log.warn("{}가 음수가 되어 0으로 조정: id={}", fieldName, id);
            }

            redisTemplate.expire(key, CACHE_TTL);
            addToSyncQueue(id);

            log.debug("{} 감소 완료: id={}, decrement={}", fieldName, id, (long) -1);

        } catch (Exception e) {
            log.error("{} 감소 실패: id={}", fieldName, id, e);
            throw new RuntimeException(fieldName + " 감소 실패", e);
        }
    }

    // ========== Sync Queue Management ==========

    /**
     * 동기화 큐에 추가
     */
    protected void addToSyncQueue(K id) {
        try {
            stringRedisTemplate.opsForSet().add(getSyncQueueKey(), id.toString());
            stringRedisTemplate.expire(getSyncQueueKey(), CACHE_TTL);
            log.debug("동기화 큐에 추가: id={}", id);
        } catch (Exception e) {
            log.warn("동기화 큐 추가 실패: id={}", id, e);
        }
    }

    /**
     * 동기화 큐에서 제거
     */
    public void removeFromSyncQueue(K id) {
        try {
            Long removedCount = stringRedisTemplate.opsForSet().remove(getSyncQueueKey(), id.toString());
            if (removedCount != null && removedCount > 0) {
                log.debug("동기화 큐에서 제거: id={}", id);
            }
        } catch (Exception e) {
            log.warn("동기화 큐 제거 실패: id={}", id, e);
        }
    }

    // ========== Utility Methods ==========

    /**
     * 캐시 키 생성
     */
    protected String generateCacheKey(K id) {
        return getCacheKeyPrefix() + id;
    }

    /**
     * ID 문자열 파싱 (기본 구현: Long 타입 가정)
     */
    @SuppressWarnings("unchecked")
    protected K parseId(String idStr) {
        try {
            return (K) Long.valueOf(idStr);
        } catch (NumberFormatException e) {
            log.warn("잘못된 ID 형식: {}", idStr);
            return null;
        }
    }
}
