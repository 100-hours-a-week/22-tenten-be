package com.kakaobase.snsapp.domain.posts.service;

import com.kakaobase.snsapp.domain.posts.entity.Post;
import com.kakaobase.snsapp.domain.posts.exception.PostException;
import com.kakaobase.snsapp.domain.posts.repository.PostRepository;
import com.kakaobase.snsapp.domain.posts.util.RedisHashUtilImpl;
import com.kakaobase.snsapp.global.common.redis.CacheRecord;
import com.kakaobase.snsapp.global.common.redis.service.ContentItemCacheService;
import com.kakaobase.snsapp.global.error.code.GeneralErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;

@Slf4j
@Service
public class PostCacheService extends ContentItemCacheService<CacheRecord.PostStatsCache, Long> {

    private final RedisHashUtilImpl redisHashUtilImpl;
    private final PostRepository postRepository;

    private static final String POST_STATS_PREFIX = "post:stats:";
    private static final String LOCK_PREFIX = "post";
    private static final String SYNC_QUEUE_KEY = "posts:need_sync";
    private static final Duration CACHE_TTL = Duration.ofHours(24);

    public PostCacheService(RedisTemplate<String, Object> redisTemplate,
                            StringRedisTemplate stringRedisTemplate,
                            RedissonClient redissonClient,
                            RedisHashUtilImpl redisHashUtilImpl,
                            PostRepository postRepository) {
        super(redisTemplate, stringRedisTemplate, redissonClient);
        this.redisHashUtilImpl = redisHashUtilImpl;
        this.postRepository = postRepository;
    }

    // ========== PUBLIC API Methods ==========

    /**
     * 게시글 캐시 조회 (부모 메서드 오버라이드)
     */
    @Override
    public CacheRecord.PostStatsCache getStatsCache(Long postId) {
        ensureCacheExistsAndCreate(postId);
        return loadCacheFromRedis(postId);
    }

    public void incrementLikeCount(Long postId) {
        incrementField(postId, "likeCount");
    }

    public void decrementLikeCount(Long postId) {
        decrementField(postId, "likeCount");
    }

    public void incrementCommentCount(Long postId) {
        incrementField(postId, "commentCount");
    }

    public void decrementCommentCount(Long postId) {
        decrementField(postId, "commentCount");
    }

    /**
     * 게시글 배치 캐시 조회
     * 캐시 미스 시 Post 객체 기반으로 캐시 생성 (DB 재조회 없이)
     */
    public Map<Long, CacheRecord.PostStatsCache> getPostStatsBatch(List<Post> posts) {
        if (posts == null || posts.isEmpty()) {
            return new HashMap<>();
        }

        Map<Long, CacheRecord.PostStatsCache> result = new HashMap<>();

        // 1. Redis Pipeline으로 배치 조회
        List<Object> rawValues = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            for (Post post : posts) {
                String key = generateCacheKey(post.getId());
                connection.hashCommands().hGetAll(key.getBytes());
            }
            return null;
        });

        // 2. 파이프라인 결과 처리 & 캐시 미스 목록 수집
        List<Long> cacheMissPostIds = new ArrayList<>();
        for (int i = 0; i < posts.size(); i++) {
            Long postId = posts.get(i).getId();
            Object rawValue = i < rawValues.size() ? rawValues.get(i) : null;

            if (rawValue != null) {
                try {
                    // PostCacheUtil을 사용해서 캐시 로드
                    String key = generateCacheKey(postId);
                    CacheRecord.PostStatsCache statsCache = redisHashUtilImpl.load(key);

                    if (statsCache != null) {
                        result.put(postId, statsCache);
                        log.debug("캐시 히트: postId={}", postId);
                    } else {
                        log.debug("캐시 값 파싱 실패: postId={}", postId);
                        cacheMissPostIds.add(postId);
                    }
                } catch (Exception e) {
                    log.warn("캐시 데이터 로드 실패: postId={}", postId, e);
                    cacheMissPostIds.add(postId);
                }
            } else {
                // 캐시 미스
                log.debug("캐시 미스: postId={}", postId);
                cacheMissPostIds.add(postId);
            }
        }

        // 3. 캐시 미스된 Post들을 기존 Post 객체로 캐싱
        if (!cacheMissPostIds.isEmpty()) {
            List<Post> cacheMissPosts = posts.stream()
                    .filter(post -> cacheMissPostIds.contains(post.getId()))
                    .toList();
            createMissingPostCache(cacheMissPosts, result);
        }

        return result;
    }

    // ========== PROTECTED Override Methods (Abstract Implementation) ==========

    /**
     * 캐시 키 접두사 반환
     */
    @Override
    protected String getCacheKeyPrefix() {
        return POST_STATS_PREFIX;
    }

    /**
     * Lock 키 접두사 반환
     */
    @Override
    protected String getLockKeyPrefix() {
        return LOCK_PREFIX;
    }

    /**
     * 동기화 큐 키 반환
     */
    @Override
    protected String getSyncQueueKey() {
        return SYNC_QUEUE_KEY;
    }

    /**
     * DB에서 Post 조회 후 캐시 생성 및 저장
     */
    @Override
    protected void saveCacheToRedis(Long postId) {
        try {
            // 1. DB에서 Post 조회
            Post post = postRepository.findById(postId)
                    .orElseThrow(() -> new PostException(GeneralErrorCode.RESOURCE_NOT_FOUND, "postId"));

            // 2. 비즈니스 로직 적용
            if (post.isDeleted()) {
                throw new PostException(GeneralErrorCode.RESOURCE_NOT_FOUND, "deleted post");
            }

            // 3. 캐시 데이터 생성
            CacheRecord.PostStatsCache cacheData = CacheRecord.PostStatsCache.builder()
                    .postId(postId)
                    .likeCount(post.getLikeCount())
                    .commentCount(post.getCommentCount())
                    .build();

            // 4. Redis에 저장
            String key = generateCacheKey(postId);
            redisHashUtilImpl.save(key, cacheData);
            redisTemplate.expire(key, CACHE_TTL);

            log.debug("DB 기반 캐시 생성 완료: postId={}, like={}, comment={}",
                    postId, post.getLikeCount(), post.getCommentCount());

        } catch (PostException e) {
            log.warn("게시글 조회 실패: postId={}, reason={}", postId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("DB 기반 캐시 생성 실패: postId={}", postId, e);
            throw new RuntimeException("캐시 생성 실패", e);
        }
    }

    /**
     * 캐시 데이터를 Redis에 저장 (캐시 데이터 직접 전달)
     */
    @Override
    protected void saveCacheToRedis(Long postId, CacheRecord.PostStatsCache cacheData) {
        try {
            String key = generateCacheKey(postId);
            redisHashUtilImpl.save(key, cacheData);
            redisTemplate.expire(key, CACHE_TTL);

            log.debug("캐시 데이터 저장 완료: postId={}, like={}, comment={}",
                    postId, cacheData.likeCount(), cacheData.commentCount());

        } catch (Exception e) {
            log.error("캐시 데이터 저장 실패: postId={}", postId, e);
            throw new RuntimeException("캐시 저장 실패", e);
        }
    }

    /**
     * Redis에서 캐시 로드
     */
    @Override
    protected CacheRecord.PostStatsCache loadCacheFromRedis(Long postId) {
        try {
            String key = generateCacheKey(postId);
            CacheRecord.PostStatsCache cache = redisHashUtilImpl.load(key);

            // 데이터 검증 (필요 시)
            if (cache != null) {
                // 데이터 무결성 체크
                if (cache.postId() == null || cache.postId() <= 0) {
                    log.warn("캐시 데이터 무결성 오류: postId={}", postId);
                    return null;
                }

                // 음수 값 보정
                if (cache.likeCount() < 0 || cache.commentCount() < 0) {
                    log.warn("캐시 데이터 음수 값 발견: postId={}, like={}, comment={}",
                            postId, cache.likeCount(), cache.commentCount());
                    cache = CacheRecord.PostStatsCache.builder()
                            .postId(cache.postId())
                            .likeCount(Math.max(0, cache.likeCount()))
                            .commentCount(Math.max(0, cache.commentCount()))
                            .build();
                }

                log.debug("캐시 로드 성공: postId={}", postId);
            }

            return cache;

        } catch (Exception e) {
            log.error("캐시 로드 실패: postId={}", postId, e);
            return null;
        }
    }

    // ========== PRIVATE Helper Methods ==========

    /**
     * Post 객체들로부터 캐시 생성 (DB 재조회 없이)
     */
    private void createMissingPostCache(List<Post> posts, Map<Long, CacheRecord.PostStatsCache> result) {
        log.info("캐시 미스 {} 개 게시글을 Post 객체 기반으로 캐싱", posts.size());

        for (Post post : posts) {
            try {
                // Post 객체에서 직접 캐시 데이터 생성
                CacheRecord.PostStatsCache cacheData = CacheRecord.PostStatsCache.builder()
                        .postId(post.getId())
                        .likeCount(post.getLikeCount())
                        .commentCount(post.getCommentCount())
                        .build();

                // 캐시 존재 보장 후 저장
                ensureCacheExistsAndCreate(post.getId(), cacheData);

                // 결과에 추가
                result.put(post.getId(), cacheData);

                log.debug("Post 기반 캐싱 완료: postId={}, like={}, comment={}",
                        post.getId(), post.getLikeCount(), post.getCommentCount());

            } catch (Exception e) {
                log.error("Post 기반 캐싱 실패: postId={}", post.getId(), e);
                // 개별 실패는 무시하고 계속 진행
            }
        }
    }
}