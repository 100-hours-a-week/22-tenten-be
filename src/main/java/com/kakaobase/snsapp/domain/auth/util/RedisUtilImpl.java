package com.kakaobase.snsapp.domain.auth.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kakaobase.snsapp.global.common.redis.CacheRecord;
import com.kakaobase.snsapp.global.common.redis.RedisHashUtil;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisUtilImpl extends RedisHashUtil<CacheRecord.UserAuthCache> {

    public RedisUtilImpl(RedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper) {
        super(redisTemplate, objectMapper);
    }

    @Override
    public void save(String key, CacheRecord.UserAuthCache cache) {
        putAll(key, cache);
    }

    @Override
    public CacheRecord.UserAuthCache load(String key) {
        return getAll(key, CacheRecord.UserAuthCache.class);
    }
}
