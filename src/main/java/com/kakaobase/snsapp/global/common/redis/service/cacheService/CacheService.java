package com.kakaobase.snsapp.global.common.redis.service.cacheService;

import com.kakaobase.snsapp.global.common.redis.error.CacheException;

import java.util.List;
import java.util.Map;

public interface CacheService<K, V, T> {

    V findBy(K id) throws CacheException;

    void incrementField(K id, String field) throws CacheException;

    void decrementField(K id, String field) throws CacheException;

    Map<K, V> findAllByItems(List<T> items) throws CacheException;

    Map<K, V> findAllById(List<K> ids) throws CacheException;

    void delete(K id) throws CacheException;
}
