package com.kakaobase.snsapp.global.common.redis.service.cacheService;

import com.kakaobase.snsapp.global.common.redis.error.CacheErrorCode;
import com.kakaobase.snsapp.global.common.redis.error.CacheException;
import com.kakaobase.snsapp.global.common.redis.service.cacheSyncService.AbstractCacheSyncService;
import com.kakaobase.snsapp.global.common.redis.util.CacheUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractCacheService<V, T> implements CacheService<Long, V, T>{

    protected final RedisTemplate<String, Object> redisTemplate;
    protected final AbstractCacheSyncService<V> cacheSyncService;
    protected final CacheUtil<String, V> cacheUtil;

    /**
     * 캐시 키 반환 (예: "post:stats: + 123")
     */
    protected abstract String generateCacheKey(Long id);

    @Override
    public V findBy(Long id) throws CacheException {
        checkCacheAndWriteBack(id);
        return cacheUtil.load(generateCacheKey(id));
    }

    @Override
    public void incrementField (Long id, String field) throws CacheException {
        checkCacheAndWriteBack(id);
        incrementFieldAndSync(id, field);
    }

    @Override
    public void decrementField(Long id, String field) throws CacheException{
        checkCacheAndWriteBack(id);
        decrementFieldAndSync(id, field);
    }

    @Override
    public void delete(Long id){
        try {
            String key = generateCacheKey(id);
            cacheUtil.delete(key);
            cacheSyncService.removeFromSyncList(key);
            log.debug("캐시 삭제 완료: id={}", id);
        } catch (Exception e) {
            log.error("캐시 삭제 실패: id={}", id, e);
        }
    }

    @Override
    public Map<Long, V> findAllByItems(List<T> items) throws CacheException {
        if (items == null || items.isEmpty()){
            log.warn("캐시중: 불러올 Entity리스트가 비어있음");
            return Map.of();
        }

        // 1. ID 추출
        List<Long> ids = items.stream()
                .map(this::extractId)
                .toList();

        // 2. Redis 키 생성
        List<String> keys = ids.stream()
                .map(this::generateCacheKey)
                .toList();

        // 3. 캐시에서 일괄 조회
        Map<String, V> loaded = cacheUtil.loadBatch(keys);

        // 4. 누락된 값이 있으면 저장
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            Long id = ids.get(i);
            T item = items.get(i);

            if (loaded.get(key) == null) {
                cacheUtil.runWithLock(key, () -> saveByEntity(id, item));
                loaded.put(key, cacheUtil.load(key));
            }
        }

        // 5. Map<Long, V>로 반환
        return IntStream.range(0, ids.size())
                .boxed()
                .collect(Collectors.toMap(
                        ids::get,
                        i -> loaded.get(keys.get(i))
                ));
    }


    @Override
    public Map<Long, V> findAllById(List<Long> ids) throws CacheException {
        if (ids == null || ids.isEmpty()){
            log.warn("캐시중: 불러올 id리스트가 비어있음");
            return Map.of();
        }

        // 1. Redis 키 생성
        List<String> keys = ids.stream()
                .map(this::generateCacheKey)
                .toList();

        // 2. 캐시에서 일괄 조회
        Map<String, V> loaded = cacheUtil.loadBatch(keys);

        // 3. 누락된 값이 있으면 저장
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            Long id = ids.get(i);

            if (loaded.get(key) == null) {
                cacheUtil.runWithLock(key, () -> saveFromDB(id));
                loaded.put(key, cacheUtil.load(key));
            }
        }

        // 5. Map<Long, V>로 반환
        return IntStream.range(0, ids.size())
                .boxed()
                .collect(Collectors.toMap(
                        ids::get,
                        i -> loaded.get(keys.get(i))
                ));
    }

    /**
     * Entity에서 id추출
     */
    protected abstract Long extractId(T item);

    protected void checkCacheAndWriteBack(Long id) throws CacheException {
        if(!cacheUtil.existsCache(generateCacheKey(id))){
            cacheUtil.runWithLock(generateCacheKey(id),()->saveFromDB(id));
        }
    }

    /**
     * ID로 레포지토리 조회 후 캐시에 저장
     */
    protected abstract void saveFromDB(Long id);

    /**
     * 매개 변수를 기반으로 캐시 생성
     */
    protected abstract void saveByEntity(Long id, T item);


    protected abstract Duration getTTL();


    // ========== Cache Operations ==========

    /**
     * 필드 값 증가 및 동기화 큐 추가
     */
    protected void incrementFieldAndSync(Long id, String fieldName) throws CacheException{
        try {
            String key = generateCacheKey(id);
            redisTemplate.opsForHash().increment(key, fieldName, 1);
            redisTemplate.expire(key, getTTL());
            cacheSyncService.addToSyncList(key);

            log.debug("{} 증가 완료: id={}, increment={}", fieldName, id, (long) 1);

        } catch (Exception e) {
            log.error("{} 증가 실패: id={}", fieldName, id, e);
            throw new CacheException(CacheErrorCode.FIELD_UPDATE_ERROR, fieldName + " 증가 실패");
        }
    }

    /**
     * 필드 값 감소 및 동기화 큐 추가 (음수 방지)
     */
    protected void decrementFieldAndSync(Long id, String fieldName) throws CacheException{
        try {
            String key = generateCacheKey(id);
            Long newValue = redisTemplate.opsForHash().increment(key, fieldName, -(long) -1);

            // 음수 방지
            if (newValue < 0) {
                redisTemplate.opsForHash().put(key, fieldName, "0");
                log.warn("{}가 음수가 되어 0으로 조정: id={}", fieldName, id);
            }

            redisTemplate.expire(key, getTTL());
            cacheSyncService.addToSyncList(key);

            log.debug("{} 감소 완료: id={}, decrement={}", fieldName, id, (long) -1);

        } catch (Exception e) {
            log.error("{} 감소 실패: id={}", fieldName, id, e);
            throw new CacheException(CacheErrorCode.FIELD_UPDATE_ERROR, fieldName + " 감소 실패");
        }
    }
}
