package com.kakaobase.snsapp.domain.posts.service;

import com.kakaobase.snsapp.domain.posts.entity.Post;
import com.kakaobase.snsapp.domain.posts.exception.PostException;
import com.kakaobase.snsapp.domain.posts.repository.PostRepository;
import com.kakaobase.snsapp.domain.posts.util.PostCacheUtil;
import com.kakaobase.snsapp.domain.posts.util.PostLuaScripts;
import com.kakaobase.snsapp.global.common.redis.CacheRecord;
import com.kakaobase.snsapp.global.error.code.GeneralErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final PostCacheUtil postCacheUtil;
    private final PostLuaScripts postLuaScripts;

    private static final String POST_STATS_PREFIX = "post:stats:";
    private static final String POSTS_NEED_SYNC = "posts:need_sync";
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
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
            String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);

            Long result = redisTemplate.execute(
                    postLuaScripts.getIncrementLikeScript(),
                    Collections.singletonList(key),
                    timestamp, "1"  // 항상 1씩 증가
            );

            if (result == 1) {
                // TTL 갱신 (Sliding Expiration)
                redisTemplate.expire(key, Duration.ofHours(24));

                // 동기화 필요 목록에 추가
                addToSyncNeeded(postId);

                log.debug("좋아요 수 증가 완료: postId={}", postId);
            } else {
                log.warn("좋아요 증가 실패 - 캐시 초기화 필요: postId={}", postId);
            }

        } catch (Exception e) {
            log.error("좋아요 수 증가 실패: postId={}, error={}", postId, e.getMessage());
        }
    }

    /**
     * 좋아요 수 감소 (좋아요 취소 시)
     */
    public void decrementLikeCount(Long postId) {
        try {
            String key = POST_STATS_PREFIX + postId;
            String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);

            Long result = redisTemplate.execute(
                    postLuaScripts.getDecrementLikeScript(),
                    Collections.singletonList(key),
                    timestamp, "1"  // 항상 1씩 감소
            );

            if (result == 1) {
                // TTL 갱신 (Sliding Expiration)
                redisTemplate.expire(key, Duration.ofHours(24));

                // 동기화 필요 목록에 추가
                addToSyncNeeded(postId);

                log.debug("좋아요 수 감소 완료: postId={}", postId);
            } else {
                log.warn("좋아요 감소 실패 - 캐시 초기화 필요: postId={}", postId);
            }

        } catch (Exception e) {
            log.error("좋아요 수 감소 실패: postId={}, error={}", postId, e.getMessage());
        }
    }

    /**
     * 댓글 수 증가 (댓글 생성 시)
     */
    public void incrementCommentCount(Long postId) {
        try {
            String key = POST_STATS_PREFIX + postId;
            String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);

            Long result = redisTemplate.execute(
                    postLuaScripts.getIncrementCommentScript(),
                    Collections.singletonList(key),
                    timestamp, "1"  // 항상 1씩 증가
            );

            if (result == 1) {
                // TTL 갱신 (Sliding Expiration)
                redisTemplate.expire(key, Duration.ofHours(24));

                // 동기화 필요 목록에 추가
                addToSyncNeeded(postId);

                log.debug("댓글 수 증가 완료: postId={}", postId);
            } else {
                log.warn("댓글 증가 실패 - 캐시 초기화 필요: postId={}", postId);
            }

        } catch (Exception e) {
            log.error("댓글 수 증가 실패: postId={}, error={}", postId, e.getMessage());
        }
    }

    /**
     * 댓글 수 감소 (댓글 삭제 시)
     */
    public void decrementCommentCount(Long postId) {
        try {
            String key = POST_STATS_PREFIX + postId;
            String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);

            Long result = redisTemplate.execute(
                    postLuaScripts.getDecrementCommentScript(),
                    Collections.singletonList(key),
                    timestamp, "1"  // 항상 1씩 감소
            );

            if (result == 1) {
                // TTL 갱신 (Sliding Expiration)
                redisTemplate.expire(key, Duration.ofHours(24));

                // 동기화 필요 목록에 추가
                addToSyncNeeded(postId);

                log.debug("댓글 수 감소 완료: postId={}", postId);
            } else {
                log.warn("댓글 감소 실패 - 캐시 초기화 필요: postId={}", postId);
            }

        } catch (Exception e) {
            log.error("댓글 수 감소 실패: postId={}, error={}", postId, e.getMessage());
        }
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