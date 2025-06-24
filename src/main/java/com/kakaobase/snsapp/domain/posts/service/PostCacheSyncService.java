package com.kakaobase.snsapp.domain.posts.service;

import com.kakaobase.snsapp.domain.posts.util.PostCacheUtil;
import com.kakaobase.snsapp.global.common.redis.CacheRecord;
import com.kakaobase.snsapp.global.common.redis.service.cacheSyncService.AbstractCacheSyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;

@Service
@Slf4j
public class PostCacheSyncService extends AbstractCacheSyncService<CacheRecord.PostStatsCache> {


    private static final String SYNC_QUEUE_KEY = "posts:need_sync";
    private static final Duration CACHE_TTL = Duration.ofHours(24);

    public PostCacheSyncService(StringRedisTemplate stringRedisTemplate,
                                PostCacheUtil cacheUtil,
                                JdbcTemplate jdbcTemplate) {
        super(stringRedisTemplate, cacheUtil, jdbcTemplate);
    }

    @Override
    protected String getSyncKey() {
        return SYNC_QUEUE_KEY;
    }

    @Override
    protected Duration getTTL() {
        return CACHE_TTL;
    }

    @Override
    protected Object[] extractSqlParameters(CacheRecord.PostStatsCache cache) {
        return new Object[]{
                cache.likeCount(),
                cache.commentCount(),
                cache.postId()
        };
    }

    @Override
    protected Object extractEntityId(CacheRecord.PostStatsCache cache) {
        return cache.postId();
    }

    @Override
    protected int[] executeJdbcBatchUpdate(List<Object[]> batchArgs) {
        String sql = """
            UPDATE posts 
            SET like_count = ?, 
                comment_count = ?,
                updated_at = NOW()
            WHERE id = ? AND deleted_at IS NULL
            """;

        return jdbcTemplate.batchUpdate(sql, batchArgs);
    }
}