package com.kakaobase.snsapp.domain.auth.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kakaobase.snsapp.global.common.redis.CacheRecord;
import com.kakaobase.snsapp.global.common.redis.util.AbstractCacheUtil;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class AuthCacheUtil extends AbstractCacheUtil<CacheRecord.UserAuthCache> {

    public AuthCacheUtil(
            RedisTemplate<String, Object> redisTemplate,
            ObjectMapper objectMapper,
            RedissonClient redissonClient) {
        super(redisTemplate, objectMapper, redissonClient);
    }

    @Override
    protected Class<CacheRecord.UserAuthCache> getType() {
        return CacheRecord.UserAuthCache.class;
    }
}
