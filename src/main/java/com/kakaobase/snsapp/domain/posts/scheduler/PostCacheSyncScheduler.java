package com.kakaobase.snsapp.domain.posts.scheduler;

import com.kakaobase.snsapp.domain.posts.service.PostCacheService;
import com.kakaobase.snsapp.domain.posts.service.PostCacheSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 게시글 통계 캐시-DB 동기화 스케줄러
 * - 1분마다 실행
 * - Redisson 분산 락 사용
 * - 실제 동기화 로직은 PostCacheSyncService에 위임
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PostCacheSyncScheduler {

    private final PostCacheService postCacheService;
    private final RedissonClient redissonClient;
    private final PostCacheSyncService postCacheSyncService;

    private static final String LOCK_KEY = "post-stats-sync-lock";
    private static final int WAIT_TIME_SECONDS = 10;
    private static final int LEASE_TIME_SECONDS = 60;

    /**
     * 1분마다 실행되는 메인 동기화 스케줄러
     */
    @Scheduled(fixedRate = 60000) // 1분마다 실행
    public void synchronizePostStats() {
        long startTime = System.currentTimeMillis();

        try {
            log.debug("게시글 통계 동기화 스케줄러 시작");

            // 1. 동기화 대상 조회
            List<Long> postIds = getPostsToSync();
            if (postIds.isEmpty()) {
                log.debug("동기화 필요한 게시글 없음");
                return;
            }

            log.info("동기화 시작 - 대상 게시글: {} 개", postIds.size());

            // 2. 분산 락 획득 후 동기화 실행
            RLock lock = acquireDistributedLock();
            if (lock != null) {
                try {
                    // PostCacheSyncService에 실제 동기화 로직 위임
                    postCacheSyncService.executeSynchronization(postIds);
                } finally {
                    releaseDistributedLock(lock);
                }
            } else {
                log.warn("분산 락 획득 실패로 동기화 스킵 - 대상 게시글: {} 개", postIds.size());
            }

        } catch (Exception e) {
            log.error("동기화 스케줄러 실행 중 예외 발생", e);
        } finally {
            long endTime = System.currentTimeMillis();
            log.debug("게시글 통계 동기화 스케줄러 완료 - 소요시간: {}ms", endTime - startTime);
        }
    }

    /**
     * 동기화 대상 게시글 목록 조회
     */
    private List<Long> getPostsToSync() {
        try {
            return postCacheService.getItemsNeedingSync();
        } catch (Exception e) {
            log.error("동기화 대상 조회 실패", e);
            return Collections.emptyList();
        }
    }

    /**
     * Redisson 분산 락 획득
     */
    private RLock acquireDistributedLock() {
        try {
            RLock lock = redissonClient.getLock(LOCK_KEY);
            boolean acquired = lock.tryLock(WAIT_TIME_SECONDS, LEASE_TIME_SECONDS, TimeUnit.SECONDS);

            if (acquired) {
                log.debug("분산 락 획득 성공");
                return lock;
            } else {
                log.warn("분산 락 획득 실패 - 다른 인스턴스에서 실행 중");
                return null;
            }

        } catch (Exception e) {
            log.error("분산 락 획득 중 예외 발생", e);
            return null;
        }
    }

    /**
     * Redisson 분산 락 해제
     */
    private void releaseDistributedLock(RLock lock) {
        try {
            if (lock != null && lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("분산 락 해제 완료");
            }
        } catch (Exception e) {
            log.warn("분산 락 해제 중 예외 발생", e);
        }
    }
}