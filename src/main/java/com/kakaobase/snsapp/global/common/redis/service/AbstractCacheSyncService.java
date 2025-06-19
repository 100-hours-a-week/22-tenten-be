package com.kakaobase.snsapp.global.common.redis.service;

import com.kakaobase.snsapp.global.common.redis.util.CacheUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractCacheSyncService<V> implements CacheSyncService<String> {

    protected final StringRedisTemplate stringRedisTemplate;
    protected final CacheUtil<String, V> cacheUtil;

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
    public void syncCacheToDB(){
        List<String> keys = getListNeedingSync();
        if(keys == null || keys.isEmpty()){
            log.info("동기화 할 목록 없음. 동기화 종료");
            return;
        }
        Map<String, V> loaded = cacheUtil.loadBatch(keys);



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
}
