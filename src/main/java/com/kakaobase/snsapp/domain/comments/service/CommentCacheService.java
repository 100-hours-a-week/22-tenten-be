package com.kakaobase.snsapp.domain.comments.service;


import com.kakaobase.snsapp.domain.comments.dto.CommentResponseDto;
import com.kakaobase.snsapp.domain.comments.repository.CommentRepository;
import com.kakaobase.snsapp.domain.comments.util.CommentCacheUtil;
import com.kakaobase.snsapp.domain.posts.exception.PostException;
import com.kakaobase.snsapp.global.common.redis.CacheRecord;
import com.kakaobase.snsapp.global.common.redis.error.CacheException;
import com.kakaobase.snsapp.global.common.redis.service.cacheService.AbstractCacheService;
import com.kakaobase.snsapp.global.error.code.GeneralErrorCode;
import lombok.extern.slf4j.Slf4j;
import com.kakaobase.snsapp.domain.comments.entity.Comment;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
public class CommentCacheService extends AbstractCacheService<CacheRecord.CommentStatsCache, CommentResponseDto.CommentInfo> {

    private static final String COMMENT_CACHE_PREFIX = "comment:stats:";
    private static final Duration CACHE_TTL = Duration.ofHours(24);
    private final CommentRepository commentRepository;

    public CommentCacheService(RedisTemplate<String, Object> redisTemplate,
                               CommentCacheSyncService commentCacheSyncService,
                               CommentCacheUtil cacheUtil,
                               CommentRepository commentRepository) {
        super(redisTemplate, commentCacheSyncService, cacheUtil);
        this.commentRepository = commentRepository;
    }

    @Override
    protected String generateCacheKey(Long id) {
        return COMMENT_CACHE_PREFIX + id;
    }

    @Override
    protected Long extractId(CommentResponseDto.CommentInfo commentInfo) {
        return commentInfo.id();
    }

    @Override
    protected void saveFromDB(Long id) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(()-> new PostException(GeneralErrorCode.RESOURCE_NOT_FOUND, "postId"));

        var cacheData = CacheRecord.CommentStatsCache.builder()
                .commentId(comment.getId())
                .likeCount(comment.getLikeCount())
                .recommentCount(comment.getRecommentCount())
                .build();

        cacheUtil.save(generateCacheKey(id), cacheData);
    }

    @Override
    protected void saveByEntity(Long id, CommentResponseDto.CommentInfo commentInfo) {
        var cacheData = CacheRecord.CommentStatsCache.builder()
                .commentId(commentInfo.id())
                .likeCount(commentInfo.likeCount())
                .recommentCount(commentInfo.recommentCount())
                .build();

        cacheUtil.save(generateCacheKey(id), cacheData);
    }

    @Override
    protected Duration getTTL() {
        return CACHE_TTL;
    }

    public void incrementLikeCount(Long commentId) throws CacheException {
        incrementField(commentId, "likeCount");
    }

    public void decrementLikeCount(Long commentId) throws CacheException {
        decrementField(commentId, "likeCount");
    }

    public void incrementCommentCount(Long commentId) throws CacheException{
        incrementField(commentId, "recommentCount");
    }

    public void decrementCommentCount(Long commentId) throws CacheException {
        decrementField(commentId, "recommentCount");
    }
}
