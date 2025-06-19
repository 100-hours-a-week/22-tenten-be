package com.kakaobase.snsapp.domain.posts.service;

import com.kakaobase.snsapp.domain.posts.util.PostCacheUtil;
import com.kakaobase.snsapp.global.common.redis.CacheRecord;
import com.kakaobase.snsapp.global.common.redis.service.AbstractCacheSyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 게시글 통계 캐시-DB 동기화 서비스
 * - 실제 동기화 로직 처리
 * - 벌크 업데이트로 성능 최적화
 * - 트랜잭션 처리
 */
@Slf4j
@Service
public class PostCacheSyncService extends AbstractCacheSyncService<CacheRecord.PostStatsCache> {

    private static final String SYNC_QUEUE_KEY = "posts:need_sync";
    private static final Duration CACHE_TTL = Duration.ofHours(24);

    public PostCacheSyncService(
            StringRedisTemplate stringRedisTemplate,
            PostCacheUtil cacheUtil) {
        super(stringRedisTemplate, cacheUtil);
    }

    @Override
    protected String getSyncKey() {
        return SYNC_QUEUE_KEY;
    }

    @Override
    protected Duration getTTL() {
        return getTTL();
    }
}