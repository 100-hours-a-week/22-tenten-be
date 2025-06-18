package com.kakaobase.snsapp.domain.posts.service;

import com.kakaobase.snsapp.global.common.redis.CacheRecord;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 게시글 통계 캐시-DB 동기화 서비스
 * - 실제 동기화 로직 처리
 * - 벌크 업데이트로 성능 최적화
 * - 트랜잭션 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostCacheSyncService {

    private final PostCacheService postCacheService;
    private final EntityManager entityManager;

    private static final int BATCH_SIZE = 1000;

    /**
     * 실제 동기화 로직 실행 (트랜잭션 적용)
     */
    @Transactional
    public void executeSynchronization(List<Long> postIds) {
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
            throw e; // 트랜잭션 롤백을 위해 예외 재발생
        }
    }

    /**
     * 단일 배치 처리
     */
    private int processBatch(List<Long> batchPostIds) {
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
     * Redis에서 캐시 데이터 조회
     */
    private Map<Long, CacheRecord.PostStatsCache> fetchPostCache(List<Long> postIds) {
        Map<Long, CacheRecord.PostStatsCache> result = new HashMap<>();

        for (Long postId : postIds) {
            try {
                CacheRecord.PostStatsCache cache = postCacheService.getStatsCache(postId);
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
                postCacheService.removeFromSyncQueue(postId);
            } catch (Exception e) {
                log.warn("동기화 완료 처리 실패 - postId: {}, error: {}", postId, e.getMessage());
            }
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