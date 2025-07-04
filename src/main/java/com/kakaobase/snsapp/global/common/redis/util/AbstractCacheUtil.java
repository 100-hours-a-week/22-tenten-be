package com.kakaobase.snsapp.global.common.redis.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kakaobase.snsapp.global.common.redis.error.CacheErrorCode;
import com.kakaobase.snsapp.global.common.redis.error.CacheException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractCacheUtil<V> implements CacheUtil<String, V> {

    protected final RedisTemplate<String, Object> redisTemplate;
    protected final ObjectMapper objectMapper;
    protected final RedissonClient redissonClient;

    protected abstract Class<V> getType();

    @Value("${lock.wait.millis:1000}")
    private long lockWaitTimeMillis;

    @Value("${lock.timeout.millis:3000}")
    private long lockTimeoutMillis;


    @Override
    public void save(String key, V value) {
        Map<String, Object> map = objectMapper.convertValue(value, new TypeReference<>() {});
        redisTemplate.opsForHash().putAll(key, map);
    }

    @Override
    public V load(String key) {
        Map<Object, Object> map = redisTemplate.opsForHash().entries(key);
        return objectMapper.convertValue(map, getType());
    }

    @Override
    public void delete(String key){
        redisTemplate.delete(key);
    }

    @Override
    public boolean existsCache(String key) {
        try {
            return redisTemplate.hasKey(key);
        } catch (Exception e) {
            log.warn("캐시 존재 여부 확인 실패: id={}", key, e);
            return false;
        }
    }

    @Override
    public Map<String, V> loadBatch(List<String> keys){
        List<Object> rawValues = redisTemplate.executePipelined((RedisCallback<Object>) conn -> {
            for (String key : keys) {
                conn.hashCommands().hGetAll(key.getBytes(StandardCharsets.UTF_8));
            }
            return null;
        });

        Map<String, V> result = new HashMap<>();
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            Object raw = rawValues.get(i);

            if (raw instanceof Map<?, ?> rawMap && !rawMap.isEmpty()) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<Object, Object> castedMap = (Map<Object, Object>) rawMap;
                    V converted = objectMapper.convertValue(castedMap, getType());
                    result.put(key, converted);

                    log.debug("✅ 캐시 변환 성공: key={}, result={}", key, converted);
                } catch (Exception e) {
                    log.warn("❌ 캐시 변환 실패: key={}", key, e);
                    result.put(key, null);
                }
            } else {
                result.put(key, null);
            }
        }
        return result;
    }

    @Override
    public void runWithLock(String cacheKey, Runnable action) throws CacheException{
        RLock lock = redissonClient.getLock("lock"+cacheKey);
        boolean acquired = false;

        try {
            acquired = lock.tryLock(lockWaitTimeMillis, lockTimeoutMillis, TimeUnit.MILLISECONDS);

            // 락 획득 실패시 early return
            if (!acquired) {
                throw new CacheException(CacheErrorCode.LOCK_ACQUISITION_FAIL);
            }

            // 캐시가 이미 존재하는 경우 early return
            if (existsCache(cacheKey)) {
                log.info("해당 캐시는 이미 존재 {}", cacheKey);
                throw new CacheException(CacheErrorCode.CACHE_ALREADY_EXISTS);
            }

            // 정상 처리
            action.run();

        } catch (Exception e) {
            log.error("락 수행 중 예외 발생", e);
            throw new CacheException(CacheErrorCode.LOCK_ERROR);
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                try {
                    lock.unlock();
                } catch (IllegalMonitorStateException e) {
                    // ✅ 이미 해제된 락이면 무시 (정상적인 상황)
                    log.debug("락이 이미 해제됨 (leaseTime 만료): {}", cacheKey);
                }
            }
        }
    }
}

