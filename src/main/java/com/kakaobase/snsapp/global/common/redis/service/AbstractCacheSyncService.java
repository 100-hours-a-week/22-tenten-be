package com.kakaobase.snsapp.global.common.redis.service;

import com.kakaobase.snsapp.global.common.redis.util.CacheUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StopWatch;

import java.time.Duration;
import java.util.*;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractCacheSyncService<V> implements CacheSyncService<String> {

    protected final StringRedisTemplate stringRedisTemplate;
    protected final CacheUtil<String, V> cacheUtil;
    protected final JdbcTemplate jdbcTemplate;

    /**
     * 동기화 큐 키 반환 (예: "posts:need_sync", "comments:need_sync")
     */
    protected abstract String getSyncKey();
    protected abstract Duration getTTL();

    @Override
    public void addToSyncList(String value){
        stringRedisTemplate.opsForSet().add(getSyncKey(), value);
        stringRedisTemplate.expire(getSyncKey(), getTTL());
    }

    @Override
    public void removeFromSyncList(String key){
        try {
            Long removedCount = stringRedisTemplate.opsForSet().remove(getSyncKey(), key);
            if (removedCount != null && removedCount > 0) {
                log.debug("동기화 큐에서 제거: key={}", key);
            }
        } catch (Exception e) {
            log.warn("동기화 큐 제거 실패: id={}", key, e);
        }
    }

    @Override
    public void removeFromSyncList(List<String> keys) {
        if (keys.isEmpty()) {
            return;
        }

        try {
            String[] keyArray = keys.toArray(new String[0]);
            Long removed = stringRedisTemplate.opsForSet().remove(getSyncKey(), keyArray);

            log.info("🧹 [{}] 동기화 큐에서 제거: {} 개", getClass().getSimpleName(), removed);

        } catch (Exception e) {
            log.error("💥 [{}] 동기화 큐 제거 실패", getClass().getSimpleName(), e);
        }
    }

    @Override
    public void syncCacheToDB(){
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        try {
            log.info("🔄 [{}] 배치 동기화 시작", getClass().getSimpleName());

            // 1️⃣ 동기화 필요한 키 목록 조회
            List<String> keys = getListNeedingSync();
            if(keys == null || keys.isEmpty()){
                log.info("📭 [{}] 동기화 할 목록 없음. 동기화 종료", getClass().getSimpleName());
                return;
            }

            log.info("📋 [{}] 동기화 대상: {} 개", getClass().getSimpleName(), keys.size());

            // 2️⃣ 배치로 캐시 로드
            Map<String, V> loaded = cacheUtil.loadBatch(keys);

            if (loaded.isEmpty()) {
                log.warn("⚠️ [{}] 로드된 캐시 없음", getClass().getSimpleName());
                return;
            }

            log.debug("📦 [{}] 캐시 로드 완료: {}/{} 개",
                    getClass().getSimpleName(), loaded.size(), keys.size());

            // 3️⃣ 캐시 데이터로 직접 배치 DB 업데이트 (내부에서 로깅 처리)
            List<String> successKeys = batchUpdateToDB(loaded);

            // 4️⃣ 성공한 키들만 동기화 큐에서 제거 (실패한 키는 재시도를 위해 유지)
            if (!successKeys.isEmpty()) {
                removeFromSyncList(successKeys);
            }

            stopWatch.stop();
            log.info("✅ [{}] 배치 동기화 완료 - 소요시간: {}ms",
                    getClass().getSimpleName(), stopWatch.getTotalTimeMillis());

        } catch (Exception e) {
            stopWatch.stop();
            log.error("💥 [{}] 배치 동기화 실패 - 소요시간: {}ms",
                    getClass().getSimpleName(), stopWatch.getTotalTimeMillis(), e);
        }
    }

    protected List<String> getListNeedingSync() {
        try {
            Set<String> itemIds = stringRedisTemplate.opsForSet().members(getSyncKey());
            if (itemIds == null || itemIds.isEmpty()) {
                return Collections.emptyList();
            }
            return itemIds.stream().toList();

        } catch (Exception e) {
            log.error("동기화 필요 항목 목록 조회 실패: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    protected List<String> batchUpdateToDB(Map<String, V> cacheMap) {
        if (cacheMap.isEmpty()) {
            log.info("📭 [{}] 업데이트할 캐시 데이터 없음", getClass().getSimpleName());
            return Collections.emptyList();
        }

        try {
            log.info("🚀 [{}] DB 배치 업데이트 시작: {} 개", getClass().getSimpleName(), cacheMap.size());

            // 📦 캐시 데이터로 배치 파라미터 준비
            List<Object[]> batchArgs = new ArrayList<>();
            List<String> cacheKeys = new ArrayList<>();
            List<Object> entityIds = new ArrayList<>();

            for (Map.Entry<String, V> entry : cacheMap.entrySet()) {
                V cache = entry.getValue();
                if (cache != null) {
                    Object[] sqlParams = extractSqlParameters(cache);
                    Object entityId = extractEntityId(cache);

                    if (sqlParams != null && entityId != null) {
                        batchArgs.add(sqlParams);
                        cacheKeys.add(entry.getKey());
                        entityIds.add(entityId);
                    }
                }
            }

            if (batchArgs.isEmpty()) {
                log.warn("⚠️ [{}] 유효한 캐시 데이터 없음", getClass().getSimpleName());
                return Collections.emptyList();
            }

            // 🔥 배치 업데이트 실행
            int[] updateCounts = executeJdbcBatchUpdate(batchArgs);

            // 📊 결과 분석 및 성공한 키만 수집
            List<String> successKeys = new ArrayList<>();
            int successCount = 0;
            int failureCount = 0;

            for (int i = 0; i < updateCounts.length; i++) {
                if (updateCounts[i] > 0) {
                    successKeys.add(cacheKeys.get(i));
                    successCount++;
                } else {
                    // 업데이트가 실패한 경우
                    failureCount++;
                    log.warn("⚠️ [{}] 업데이트 실패: id={} (다음 동기화에서 재시도)",
                            getClass().getSimpleName(), entityIds.get(i));
                }
            }

            // 📈 최종 결과 로깅
            logBatchUpdateResult(successCount, failureCount, batchArgs.size());

            return successKeys;

        } catch (Exception e) {
            log.error("💥 [{}] DB 배치 업데이트 실패 - 모든 키를 다음 동기화에서 재시도",
                    getClass().getSimpleName(), e);
            return Collections.emptyList(); // 실패시 빈 리스트 반환하여 모든 키가 재시도되도록
        }
    }

    /**
     * 📈 배치 업데이트 결과 로깅
     */
    protected void logBatchUpdateResult(int successCount, int failureCount, int totalCount) {
        double successRate = totalCount > 0 ? (double) successCount / totalCount * 100 : 0;

        if (successRate >= 95.0) {
            log.info("✅ {} DB 배치 업데이트 성공 - 성공: {}, 실패: {}, 성공률: {}%",
                    getClass().getSimpleName(), successCount, failureCount, successRate);
        } else {
            log.warn("⚠️ [{}] DB 배치 업데이트 부분 실패 - 성공: {}, 실패: {}, 성공률: {}%",
                    getClass().getSimpleName(), successCount, failureCount, successRate);
        }
    }

    // ===================== 추상 메서드들 =====================

    /**
     * 캐시 객체에서 SQL 파라미터 배열 추출
     * 예: [likeCount, commentCount, postId]
     */
    protected abstract Object[] extractSqlParameters(V cache);

    /**
     * 캐시에서 엔티티 ID 추출 (로깅용)
     */
    protected abstract Object extractEntityId(V cache);

    /**
     * JDBC 배치 업데이트 실행 (SQL은 구현체에서 정의)
     */
    protected abstract int[] executeJdbcBatchUpdate(List<Object[]> batchArgs);
}
