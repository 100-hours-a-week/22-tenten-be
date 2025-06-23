package com.kakaobase.snsapp.domain.follow.service;

import com.kakaobase.snsapp.domain.follow.util.FollowCacheUtil;
import com.kakaobase.snsapp.global.common.redis.CacheRecord;
import com.kakaobase.snsapp.global.common.redis.service.cacheSyncService.AbstractCacheSyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Slf4j
@Service
public class FollowCacheSyncService extends AbstractCacheSyncService<CacheRecord.FollowStatsCache> {
    private static final String SYNC_QUEUE_KEY = "follows:need_sync";
    private static final Duration CACHE_TTL = Duration.ofHours(24);

    public FollowCacheSyncService(StringRedisTemplate redisTemplate,
                                  FollowCacheUtil cacheUtil,
                                  JdbcTemplate jdbcTemplate) {
        super(redisTemplate, cacheUtil, jdbcTemplate);
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
    protected Object[] extractSqlParameters(CacheRecord.FollowStatsCache cache) {
        return new Object[]{
                cache.followerCount(),
                cache.followingCount(),
                cache.memberId()
        };
    }

    @Override
    protected Object extractEntityId(CacheRecord.FollowStatsCache cache) {
        return cache.memberId();
    }

    @Override
    protected int[] executeJdbcBatchUpdate(List<Object[]> batchArgs) {
        String sql = """
            UPDATE members 
            SET follower_count = ?, 
                following_count = ?,
                updated_at = NOW()
            WHERE id = ? AND deleted_at IS NULL
            """;

        return jdbcTemplate.batchUpdate(sql, batchArgs);
    }
}
