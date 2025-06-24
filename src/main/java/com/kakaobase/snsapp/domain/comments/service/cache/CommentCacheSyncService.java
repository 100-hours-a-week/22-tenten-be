package com.kakaobase.snsapp.domain.comments.service.cache;

import com.kakaobase.snsapp.domain.comments.util.CommentCacheUtil;
import com.kakaobase.snsapp.global.common.redis.CacheRecord;
import com.kakaobase.snsapp.global.common.redis.service.cacheSyncService.AbstractCacheSyncService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
public class CommentCacheSyncService extends AbstractCacheSyncService<CacheRecord.CommentStatsCache> {

    private static final String SYNC_QUEUE_KEY = "comment:need_sync";
    private static final Duration CACHE_TTL = Duration.ofHours(24);

    public CommentCacheSyncService(StringRedisTemplate stringRedisTemplate,
                                   CommentCacheUtil cacheUtil,
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
    protected Object[] extractSqlParameters(CacheRecord.CommentStatsCache cache) {
        return new Object[]{
                cache.likeCount(),
                cache.recommentCount(),
                cache.commentId()
        };
    }

    @Override
    protected Object extractEntityId(CacheRecord.CommentStatsCache cache) {
        return cache.commentId();
    }

    @Override
    protected int[] executeJdbcBatchUpdate(List<Object[]> batchArgs) {
        String sql = """
            UPDATE comments 
            SET like_count = ?, 
                recomment_count = ?,
                updated_at = NOW()
            WHERE id = ? AND deleted_at IS NULL
            """;

        return jdbcTemplate.batchUpdate(sql, batchArgs);
    }
}
