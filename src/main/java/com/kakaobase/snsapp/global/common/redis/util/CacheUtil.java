package com.kakaobase.snsapp.global.common.redis.util;

import com.kakaobase.snsapp.global.common.redis.error.CacheException;

import java.util.List;
import java.util.Map;

public interface CacheUtil<K, V> {
    void save(K key, V value);
    V load(K key);
    boolean existsCache(K key);
    Map<K, V> loadBatch(List<K> keys);
    void delete(K key);
    void runWithLock(K key, Runnable action) throws CacheException;
}
