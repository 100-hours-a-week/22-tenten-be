package com.kakaobase.snsapp.global.common.redis.service.cacheService;

import java.util.List;
import java.util.Map;

public interface CacheService<K, V, T> {

    V findBy(K id);

    void incrementField(K id, String field);

    void decrementField(K id, String field);

    Map<K, V> findAllByItems(List<T> items);

    Map<K, V> findAllById(List<K> ids);

    void delete(K id);
}
