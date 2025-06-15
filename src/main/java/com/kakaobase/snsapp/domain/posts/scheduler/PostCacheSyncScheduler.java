package com.kakaobase.snsapp.domain.posts.scheduler;

import com.kakaobase.snsapp.domain.posts.service.PostCacheService;
import com.kakaobase.snsapp.global.common.redis.CacheRecord;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 게시글 통계 캐시-DB 동기화 스케줄러
 * - 1분마다 실행
 * - Redisson 분산 락 사용
 * - 벌크 업데이트로 성능 최적화
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PostCacheSyncScheduler {

    private final PostCacheService postCacheService;
    private final RedissonClient redissonClient;
    private final EntityManager entityManager;
    private static final String LOCK_KEY = "post-stats-sync-lock";
    private static final int WAIT_TIME_SECONDS = 10;
    private static final int LEASE_TIME_SECONDS = 60;
    private static final int BATCH_SIZE = 1000;

    /**
     * 1분마다 실행되는 메인 동기화 스케줄러
     */
    @Scheduled(fixedRate = 60000) // 1분마다 실행
    public void synchronizePostStats() {
        long startTime = System.currentTimeMillis();

        try {
            log.debug("게시글 통계 동기화 스케줄러 시작");


            // 2. 동기화 대상 조회
            List<Long> postIds = getPostsToSync();
            if (postIds.isEmpty()) {
                log.debug("동기화 필요한 게시글 없음");
                return;
            }

            log.info("동기화 시작 - 대상 게시글: {} 개", postIds.size());

            // 3. 분산 락 획득 후 동기화 실행
            RLock lock = acquireDistributedLock();
            if (lock != null) {
                try {
                    executeSynchronization(postIds);
                } finally {
                    releaseDistributedLock(lock);
                }
            }

        } catch (Exception e) {
            log.error("동기화 스케줄러 실행 중 예외 발생", e);
        } finally {
            long endTime = System.currentTimeMillis();
            log.debug("게시글 통계 동기화 스케줄러 완료 - 소요시간: {}ms", endTime - startTime);
        }
    }

    /**
     * 실제 동기화 로직 실행
     */
    private void executeSynchronization(List<Long> postIds) {
        try {
            // 배치 단위로 처리
            List<List<Long>> batches = partitionList(postIds);
            int totalProcessed = 0;
            int totalFailed = 0;

            for (List<Long> batch : batches) {
                try {
                    int processedCount = processBatch(batch);
                    totalProcessed += processedCount;
                    log.debug("배치 처리 완료 - 처리: {} 개", processedCount);
                } catch (Exception e) {
                    totalFailed += batch.size();
                    log.error("배치 처리 실패 - 대상: {} 개, 오류: {}", batch.size(), e.getMessage());
                }
            }

            logSyncResults(postIds.size(), totalProcessed, totalFailed);

        } catch (Exception e) {
            log.error("동기화 실행 중 예외 발생", e);
        }
    }

    /**
     * 단일 배치 처리
     */
    @Transactional
    protected int processBatch(List<Long> batchPostIds) {
        try {
            // 1. Redis에서 캐시 데이터 조회
            Map<Long, CacheRecord.PostStatsCache> cacheDataMap = fetchPostCache(batchPostIds);

            if (cacheDataMap.isEmpty()) {
                log.debug("유효한 캐시 데이터 없음 - 배치 크기: {}", batchPostIds.size());
                return 0;
            }

            // 2. DB 일괄 업데이트
            int updatedCount = bulkUpdatePostStats(cacheDataMap);

            // 3. 동기화 완료 처리
            if (updatedCount > 0) {
                removePostCacheSyncList(new ArrayList<>(cacheDataMap.keySet()));
            }

            return updatedCount;

        } catch (Exception e) {
            log.error("배치 처리 중 오류 발생 - 배치 크기: {}", batchPostIds.size(), e);
            throw e;
        }
    }

    /**
     * 동기화 대상 게시글 목록 조회
     */
    private List<Long> getPostsToSync() {
        try {
            return postCacheService.getPostsCacheNeedingSync();
        } catch (Exception e) {
            log.error("동기화 대상 조회 실패", e);
            return Collections.emptyList();
        }
    }

    /**
     * Redis에서 캐시 데이터 조회
     */
    private Map<Long, CacheRecord.PostStatsCache> fetchPostCache(List<Long> postIds) {
        Map<Long, CacheRecord.PostStatsCache> result = new HashMap<>();

        for (Long postId : postIds) {
            try {
                CacheRecord.PostStatsCache cache = postCacheService.getPostCache(postId);
                if (cache != null) {
                    result.put(postId, cache);
                } else {
                    log.debug("유효하지 않은 캐시 데이터 - postId: {}", postId);
                }
            } catch (Exception e) {
                log.warn("캐시 데이터 조회 실패 - postId: {}, error: {}", postId, e.getMessage());
            }
        }

        return result;
    }

    /**
     * DB 일괄 업데이트 (CASE WHEN 사용)
     */
    private int bulkUpdatePostStats(Map<Long, CacheRecord.PostStatsCache> cacheDataMap) {
        if (cacheDataMap.isEmpty()) {
            return 0;
        }

        try {
            StringBuilder jpql = new StringBuilder();
            jpql.append("UPDATE Post p SET ");

            // likeCount 업데이트
            jpql.append("p.likeCount = CASE p.id ");
            for (Map.Entry<Long, CacheRecord.PostStatsCache> entry : cacheDataMap.entrySet()) {
                Long postId = entry.getKey();
                Long likeCount = entry.getValue().likeCount();
                jpql.append("WHEN ").append(postId).append(" THEN ").append(likeCount).append(" ");
            }
            jpql.append("ELSE p.likeCount END, ");

            // commentCount 업데이트
            jpql.append("p.commentCount = CASE p.id ");
            for (Map.Entry<Long, CacheRecord.PostStatsCache> entry : cacheDataMap.entrySet()) {
                Long postId = entry.getKey();
                Long commentCount = entry.getValue().commentCount();
                jpql.append("WHEN ").append(postId).append(" THEN ").append(commentCount).append(" ");
            }
            jpql.append("ELSE p.commentCount END ");
            jpql.append("WHERE p.id IN :postIds");

            int updatedCount = entityManager.createQuery(jpql.toString())
                    .setParameter("postIds", cacheDataMap.keySet())
                    .executeUpdate();

            log.debug("일괄 업데이트 완료 - 업데이트된 레코드: {} 개", updatedCount);
            return updatedCount;

        } catch (Exception e) {
            log.error("일괄 업데이트 실패 - 대상: {} 개", cacheDataMap.size(), e);
            throw e;
        }
    }

    /**
     * 동기화 완료 처리 (Redis 동기화 목록에서 제거)
     */
    private void removePostCacheSyncList(List<Long> synchronizedPostIds) {
        for (Long postId : synchronizedPostIds) {
            try {
                postCacheService.removeFromSyncNeeded(postId);
            } catch (Exception e) {
                log.warn("동기화 완료 처리 실패 - postId: {}, error: {}", postId, e.getMessage());
            }
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

    /**
     * 동기화 결과 로깅
     */
    private void logSyncResults(int totalCount, int successCount, int failureCount) {
        if (totalCount > 0) {
            double successRate = (double) successCount / totalCount * 100;
            log.info("동기화 완료 - 전체: {}, 성공: {}, 실패: {}, 성공률: {:.1f}%",
                    totalCount, successCount, failureCount, successRate);
        }
    }

    /**
     * 리스트를 지정된 크기의 배치로 분할
     */
    private <T> List<List<T>> partitionList(List<T> list) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += BATCH_SIZE) {
            partitions.add(list.subList(i, Math.min(i + BATCH_SIZE, list.size())));
        }
        return partitions;
    }
}