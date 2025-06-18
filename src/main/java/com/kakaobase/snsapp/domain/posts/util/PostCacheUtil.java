package com.kakaobase.snsapp.domain.posts.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kakaobase.snsapp.global.common.redis.CacheRecord;
import com.kakaobase.snsapp.global.common.redis.util.RedisHashUtil;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class PostCacheUtil extends RedisHashUtil<CacheRecord.PostStatsCache> {

    public PostCacheUtil(RedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper) {
        super(redisTemplate, objectMapper);
    }

    @Override
    public void save(String key, CacheRecord.PostStatsCache value) {
        putAll(key, value);
    }

    @Override
    public CacheRecord.PostStatsCache load(String key) {
        return getAll(key, CacheRecord.PostStatsCache.class);
    }
}