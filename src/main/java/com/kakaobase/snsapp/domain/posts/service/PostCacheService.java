package com.kakaobase.snsapp.domain.posts.service;

import com.kakaobase.snsapp.domain.posts.dto.PostResponseDto;
import com.kakaobase.snsapp.domain.posts.entity.Post;
import com.kakaobase.snsapp.domain.posts.exception.PostException;
import com.kakaobase.snsapp.domain.posts.repository.PostRepository;
import com.kakaobase.snsapp.domain.posts.util.PostCacheUtil;
import com.kakaobase.snsapp.global.common.redis.CacheRecord;
import com.kakaobase.snsapp.global.common.redis.service.AbstractCacheService;
import com.kakaobase.snsapp.global.error.code.GeneralErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
public class PostCacheService extends AbstractCacheService<CacheRecord.PostStatsCache, PostResponseDto.PostDetails> {

    private static final String POST_CACHE_PREFIX = "post:stats:";
    private static final Duration CACHE_TTL = Duration.ofHours(24);
    private final PostRepository postRepository;

    public PostCacheService(RedisTemplate<String, Object> redisTemplate,
                            PostCacheUtil cacheUtil,
                            PostCacheSyncService cacheSyncService,
                            PostRepository postRepository) {
        super(redisTemplate, cacheSyncService, cacheUtil);
        this.postRepository = postRepository;
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

    @Override
    protected String generateCacheKey(Long id) {
        return POST_CACHE_PREFIX + id;
    }

    @Override
    protected Long extractId(PostResponseDto.PostDetails postDetails) {
        return postDetails.id();
    }

    @Override
    protected void saveFromDB(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(()-> new PostException(GeneralErrorCode.RESOURCE_NOT_FOUND, "postId"));

        var cacheData = CacheRecord.PostStatsCache.builder()
                .postId(post.getId())
                .likeCount(post.getLikeCount())
                .commentCount(post.getCommentCount())
                .build();

        cacheUtil.save(generateCacheKey(id), cacheData);
    }

    @Override
    protected void saveByEntity(Long id, PostResponseDto.PostDetails postDetails) {
        var cacheData = CacheRecord.PostStatsCache.builder()
                .postId(postDetails.id())
                .likeCount(postDetails.likeCount())
                .commentCount(postDetails.commentCount())
                .build();

        cacheUtil.save(generateCacheKey(id), cacheData);
    }

    @Override
    protected Duration getTTL() {
        return CACHE_TTL;
    }
}