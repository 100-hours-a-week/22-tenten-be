package com.kakaobase.snsapp.domain.posts.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kakaobase.snsapp.global.common.redis.CacheRecord;
import com.kakaobase.snsapp.global.common.redis.util.AbstractCacheUtil;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PostCacheUtil extends AbstractCacheUtil<CacheRecord.PostStatsCache> {

    public PostCacheUtil(RedisTemplate<String, Object> redisTemplate,
                         ObjectMapper objectMapper,
                         RedissonClient redissonClient) {
        super(redisTemplate, objectMapper, redissonClient);
    }

    @Override
    protected Class<CacheRecord.PostStatsCache> getType() {
        return CacheRecord.PostStatsCache.class;
    }
}
