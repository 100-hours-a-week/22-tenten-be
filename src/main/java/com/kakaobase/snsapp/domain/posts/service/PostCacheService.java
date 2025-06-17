package com.kakaobase.snsapp.domain.posts.service;

import com.kakaobase.snsapp.domain.posts.entity.Post;
import com.kakaobase.snsapp.domain.posts.exception.PostException;
import com.kakaobase.snsapp.domain.posts.repository.PostRepository;
import com.kakaobase.snsapp.domain.posts.util.PostCacheUtil;
import com.kakaobase.snsapp.global.common.redis.CacheRecord;
import com.kakaobase.snsapp.global.error.code.GeneralErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final PostCacheUtil postCacheUtil;

    private static final String POST_STATS_PREFIX = "post:stats:";
    private static final String POSTS_NEED_SYNC = "posts:need_sync";
    private final PostRepository postRepository;


    public boolean existsPostCache(Long postId) {
        String key = POST_STATS_PREFIX + postId;

        try {
            return redisTemplate.hasKey(key);
        } catch (Exception e) {
            log.warn("키 존재 여부 확인 중 오류 발생: {}", key, e);
            return false;
        }
    }

    /**
     * 좋아요 수 증가 (좋아요 생성 시)
     */
    public void incrementLikeCount(Long postId) {
        try {
            String key = POST_STATS_PREFIX + postId;

            // HINCRBY로 원자적으로 likeCount 필드를 1 증가
            Long newCount = redisTemplate.opsForHash().increment(key, "likeCount", 1);

            // TTL 갱신 (Sliding Expiration)
            redisTemplate.expire(key, Duration.ofHours(24));

            // 동기화 필요 목록에 추가
            addToSyncNeeded(postId);

            log.debug("좋아요 수 증가 완료: postId={}, newCount={}", postId, newCount);

        } catch (Exception e) {
            log.error("좋아요 수 증가 실패: postId={}, error={}", postId, e.getMessage());
            throw new RuntimeException("좋아요 수 증가 실패", e);
        }
    }

    /**
     * 좋아요 수 감소 (좋아요 취소 시)
     */
    public void decrementLikeCount(Long postId) {
        try {
            String key = POST_STATS_PREFIX + postId;

            // HINCRBY로 원자적으로 likeCount 필드를 1 감소
            Long newCount = redisTemplate.opsForHash().increment(key, "likeCount", -1);

            // 음수 방지 - 0보다 작으면 0으로 설정
            if (newCount < 0) {
                redisTemplate.opsForHash().put(key, "likeCount", "0");
                newCount = 0L;
                log.warn("좋아요 수가 음수가 되어 0으로 조정: postId={}", postId);
            }

            // TTL 갱신 (Sliding Expiration)
            redisTemplate.expire(key, Duration.ofHours(24));

            // 동기화 필요 목록에 추가
            addToSyncNeeded(postId);

            log.debug("좋아요 수 감소 완료: postId={}, newCount={}", postId, newCount);

        } catch (Exception e) {
            log.error("좋아요 수 감소 실패: postId={}, error={}", postId, e.getMessage());
            throw new RuntimeException("좋아요 수 감소 실패", e);
        }
    }

    /**
     * 댓글 수 증가 (댓글 생성 시)
     */
    public void incrementCommentCount(Long postId) {
        try {
            String key = POST_STATS_PREFIX + postId;

            // HINCRBY로 원자적으로 commentCount 필드를 1 증가
            Long newCount = redisTemplate.opsForHash().increment(key, "commentCount", 1);

            // TTL 갱신 (Sliding Expiration)
            redisTemplate.expire(key, Duration.ofHours(24));

            // 동기화 필요 목록에 추가
            addToSyncNeeded(postId);

            log.debug("댓글 수 증가 완료: postId={}, newCount={}", postId, newCount);

        } catch (Exception e) {
            log.error("댓글 수 증가 실패: postId={}, error={}", postId, e.getMessage());
            throw new RuntimeException("댓글 수 증가 실패", e);
        }
    }

    /**
     * 댓글 수 감소 (댓글 삭제 시)
     */
    public void decrementCommentCount(Long postId) {
        try {
            String key = POST_STATS_PREFIX + postId;

            // HINCRBY로 원자적으로 commentCount 필드를 1 감소
            long newCount = redisTemplate.opsForHash().increment(key, "commentCount", -1);

            // 음수 방지 - 0보다 작으면 0으로 설정
            if (newCount < 0) {
                redisTemplate.opsForHash().put(key, "commentCount", "0");
                newCount = 0L;
                log.warn("댓글 수가 음수가 되어 0으로 조정: postId={}", postId);
            }

            // TTL 갱신 (Sliding Expiration)
            redisTemplate.expire(key, Duration.ofHours(24));

            // 동기화 필요 목록에 추가
            addToSyncNeeded(postId);

            log.debug("댓글 수 감소 완료: postId={}, newCount={}", postId, newCount);

        } catch (Exception e) {
            log.error("댓글 수 감소 실패: postId={}, error={}", postId, e.getMessage());
            throw new RuntimeException("댓글 수 감소 실패", e);
        }
    }


    public Map<Long, CacheRecord.PostStatsCache> getPostStatsBatch(List<Post> posts) {
        if (posts == null || posts.isEmpty()) {
            return new HashMap<>();
        }

        // 1. 캐시된 PostStatus 값을 List<Object>로 조회
        List<Object> rawValues = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            for (Post post : posts) {
                String key = POST_STATS_PREFIX + post.getId();
                // 새로운 API 사용
                connection.hashCommands().hGetAll(key.getBytes());
            }
            return null;
        });

        // 2. result에 Map<Long(postId), PostStatsCache> 형태로 저장
        Map<Long, CacheRecord.PostStatsCache> result = new HashMap<>();
        List<Long> cacheMissPostIds = new ArrayList<>();

        // Post 객체를 ID로 빠르게 찾기 위한 맵 생성
        Map<Long, Post> postMap = posts.stream()
                .collect(Collectors.toMap(Post::getId, Function.identity()));

        for (int i = 0; i < posts.size(); i++) {
            Long postId = posts.get(i).getId();
            Object rawValue = i < rawValues.size() ? rawValues.get(i) : null;

            if (rawValue != null) {
                try {
                    // PostCacheUtil을 사용해서 Object를 PostStatsCache로 변환
                    String key = POST_STATS_PREFIX + postId;
                    CacheRecord.PostStatsCache statsCache = postCacheUtil.load(key);

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
                // 3. 해당 postId에 해당하는 값이 비어있다면 cacheMissPostIds에 저장
                log.debug("캐시 미스: postId={}", postId);
                cacheMissPostIds.add(postId);
            }
        }

        // 4. cacheMissPostIds에 해당하는 Post 객체들만 처리하여 PostStatsCache 생성
        if (!cacheMissPostIds.isEmpty()) {
            log.info("캐시 미스 {} 개 게시글을 매개변수 Post 객체 기반으로 캐싱", cacheMissPostIds.size());

            // cacheMissPostIds에 해당하는 Post 객체들만 필터링
            List<Post> cacheMissPosts = cacheMissPostIds.stream()
                    .map(postMap::get)
                    .filter(Objects::nonNull)
                    .toList();

            // 캐시 미스된 게시글들만 처리
            for (Post post : cacheMissPosts) {
                var statsCache = CacheRecord.PostStatsCache.builder()
                        .postId(post.getId())
                        .likeCount(post.getLikeCount())
                        .commentCount(post.getCommentCount())
                        .build();

                // result에 반영
                result.put(post.getId(), statsCache);

                // createPostStatCache를 통해 캐싱 (내부적으로 PostCacheUtil 사용)
                createPostStatCache(post.getId(), statsCache);

                log.debug("매개변수 Post 기반으로 캐싱: postId={}, like={}, comment={}",
                        post.getId(), post.getLikeCount(), post.getCommentCount());
            }
        }

        log.debug("게시글 통계 배치 조회 완료: {} 개 (캐시 히트: {}, 미스: {})",
                posts.size(), posts.size() - cacheMissPostIds.size(), cacheMissPostIds.size());

        // 5. 최종 결과인 result 반환
        return result;
    }

    /**
     * 게시글 캐시 조회
     */
    public CacheRecord.PostStatsCache getPostCache(Long postId) {
        try {
            String key = POST_STATS_PREFIX + postId;
            redisTemplate.expire(key, Duration.ofHours(24));
            return postCacheUtil.load(key);
        } catch (Exception e) {
            log.error("게시글 통계 조회 실패: postId={}, error={}", postId, e.getMessage());
            return null;
        }
    }

    /**
     * 초기 캐시 데이터 설정 (DB에서 불러온 초기 값으로)
     */
    public void createPostStatCache(Long postId) {
        try {
            String key = POST_STATS_PREFIX + postId;
            LocalDateTime currentTime = LocalDateTime.now();

            Post post = postRepository.findById(postId)
                    .orElseThrow(()-> new PostException(GeneralErrorCode.RESOURCE_NOT_FOUND, "postId"));

            CacheRecord.PostStatsCache caheDto = CacheRecord.PostStatsCache.builder()
                    .postId(postId)
                    .likeCount(post.getLikeCount())
                    .commentCount(post.getCommentCount())
                    .build();

            // PostCacheUtil 사용
            postCacheUtil.save(key, caheDto);
            redisTemplate.expire(key, Duration.ofHours(24));

            log.debug("게시글 통계 초기화 완료: postId={}, likeCount={}, commentCount={}, time={}",
                    postId, post.getLikeCount(), post.getCommentCount(), currentTime);

        } catch (Exception e) {
            log.error("게시글 통계 초기화 실패: postId={}, error={}", postId, e.getMessage());
        }
    }

    public void createPostStatCache(Long postId, CacheRecord.PostStatsCache statsCache) {
        try {
            String key = POST_STATS_PREFIX + postId;

            // PostCacheUtil을 사용해서 캐시에 저장
            postCacheUtil.save(key, statsCache);
            redisTemplate.expire(key, Duration.ofHours(24));

            log.debug("게시글 통계 캐싱 완료: postId={}, likeCount={}, commentCount={}",
                    postId, statsCache.likeCount(), statsCache.commentCount());

        } catch (Exception e) {
            log.error("게시글 통계 캐싱 실패: postId={}, error={}", postId, e.getMessage());
        }
    }

    /**
     * 동기화가 필요한 게시글 목록 조회 (Set 방식)
     */
    public List<Long> getPostsCacheNeedingSync() {
        try {
            Set<String> postIds = stringRedisTemplate.opsForSet().members(POSTS_NEED_SYNC);

            if (postIds == null || postIds.isEmpty()) {
                log.debug("동기화 필요한 게시글 없음");
                return Collections.emptyList();
            }

            List<Long> needsSyncPosts = postIds.stream()
                    .map(postIdStr -> {
                        try {
                            return Long.valueOf(postIdStr);
                        } catch (NumberFormatException e) {
                            log.warn("잘못된 postId 형식: {}", postIdStr);
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            log.debug("동기화 필요한 게시글: {} 개", needsSyncPosts.size());
            return needsSyncPosts;

        } catch (Exception e) {
            log.error("동기화 필요 게시글 목록 조회 실패: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 캐시 삭제
     */
    public void deletePostCache(Long postId) {
        try {
            String key = POST_STATS_PREFIX + postId;
            redisTemplate.delete(key);

            // 동기화 목록에서도 제거
            removeFromSyncNeeded(postId);

            log.debug("게시글 통계 캐시 삭제 완료: postId={}", postId);

        } catch (Exception e) {
            log.error("게시글 통계 캐시 삭제 실패: postId={}, error={}", postId, e.getMessage());
        }
    }

    /**
     * 동기화 필요 목록에서 제거
     */
    public void removeFromSyncNeeded(Long postId) {
        try {
            Long removedCount = stringRedisTemplate.opsForSet().remove(POSTS_NEED_SYNC, postId.toString());

            if (removedCount != null && removedCount > 0) {
                log.debug("동기화 필요 목록에서 제거: postId={}", postId);
            } else {
                log.debug("동기화 목록에 없던 게시글: postId={}", postId);
            }

        } catch (Exception e) {
            log.warn("동기화 필요 목록 제거 실패: postId={}, error={}", postId, e.getMessage());
        }
    }

    /**
     * 동기화 필요 목록에 추가
     */
    private void addToSyncNeeded(Long postId) {
        try {
            stringRedisTemplate.opsForSet().add(POSTS_NEED_SYNC, postId.toString());

            // 동기화 필요 목록도 TTL 설정 (24시간)
            stringRedisTemplate.expire(POSTS_NEED_SYNC, Duration.ofHours(24));

            log.debug("동기화 필요 목록에 추가: postId={}", postId);

        } catch (Exception e) {
            log.warn("동기화 필요 목록 추가 실패: postId={}, error={}", postId, e.getMessage());
        }
    }
}